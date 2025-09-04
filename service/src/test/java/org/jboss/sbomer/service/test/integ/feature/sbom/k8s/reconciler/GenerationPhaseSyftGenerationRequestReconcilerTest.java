/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2023 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.sbomer.service.test.integ.feature.sbom.k8s.reconciler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.jboss.sbomer.core.features.sbom.config.Config;
import org.jboss.sbomer.core.features.sbom.config.SyftImageConfig;
import org.jboss.sbomer.core.features.sbom.enums.GenerationRequestType;
import org.jboss.sbomer.core.features.sbom.utils.SbomUtils;
import org.jboss.sbomer.core.test.TestResources;
import org.jboss.sbomer.service.feature.sbom.atlas.AtlasHandler;
import org.jboss.sbomer.service.feature.sbom.config.GenerationRequestControllerConfig;
import org.jboss.sbomer.service.feature.sbom.k8s.model.GenerationRequest;
import org.jboss.sbomer.service.feature.sbom.k8s.model.GenerationRequestBuilder;
import org.jboss.sbomer.service.feature.sbom.k8s.model.SbomGenerationPhase;
import org.jboss.sbomer.service.feature.sbom.k8s.model.SbomGenerationStatus;
import org.jboss.sbomer.service.feature.sbom.k8s.resources.Labels;
import org.jboss.sbomer.service.generator.image.controller.SyftImageController;
import org.jboss.sbomer.service.test.unit.feature.sbom.syftimage.TestControllerProfile;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import io.fabric8.knative.pkg.apis.ConditionBuilder;
import io.fabric8.kubernetes.client.server.mock.KubernetesServer;
import io.fabric8.tekton.v1beta1.ParamBuilder;
import io.fabric8.tekton.v1beta1.ParamValue;
import io.fabric8.tekton.v1beta1.TaskRun;
import io.fabric8.tekton.v1beta1.TaskRunBuilder;
import io.fabric8.tekton.v1beta1.TaskRunStatusBuilder;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.kubernetes.client.KubernetesTestServer;
import jakarta.inject.Inject;

@QuarkusTest
/*
 * Relevant in this profile we tune the settings relevant to bulkhead and retry down so the unrecoverable bulkhead
 * exception shows up quickly, in practice it should never show up
 */
@TestProfile(TestControllerProfile.class)
class GenerationPhaseSyftGenerationRequestReconcilerTest {

    @InjectMock
    GenerationRequestControllerConfig controllerConfig;

    @Inject
    SyftImageController sic;

    @InjectMock
    AtlasHandler atlasHandler;

    @KubernetesTestServer
    KubernetesServer mockServer;

    private List<Path> generatedSbomDirs;
    private ExecutorService executorService;

    @AfterEach
    void cleanup() {
        if (executorService != null) {
            executorService.shutdownNow();
        }
    }

    /*
     * Generate a number of generation requests, either rand() < max or just max
     */
    public List<GenerationRequest> generationRequests(int max, boolean useRandomCount, Path tempDir)
            throws IOException {

        when(controllerConfig.sbomDir()).thenReturn(tempDir.toString());
        List<GenerationRequest> requests = new ArrayList<>();
        generatedSbomDirs = new ArrayList<>(); // Initialize the list
        int requestCount;

        if (useRandomCount) {
            if (max <= 0) {
                throw new IllegalArgumentException("Max must be a positive number for random generation.");
            }
            requestCount = new Random().nextInt(max) + 1;
        } else {
            requestCount = max;
        }

        for (int i = 0; i < requestCount; i++) {
            String requestName = "test-generation-request-" + i;

            Path sbomDir = tempDir.resolve(requestName);
            Files.createDirectory(sbomDir);
            generatedSbomDirs.add(sbomDir); // Store the path

            Path manifestPath = sbomDir.resolve("bom.json");
            Files.writeString(manifestPath, TestResources.asString("sboms/complete_sbom.json"));

            GenerationRequest generationRequest = new GenerationRequestBuilder(GenerationRequestType.BUILD)
                    .withIdentifier(String.valueOf(i))
                    .withStatus(SbomGenerationStatus.GENERATING)
                    .withConfig(Config.fromString("{\"type\": \"syft-image\"}", SyftImageConfig.class))
                    .build();

            generationRequest.getMetadata().setName(requestName);
            requests.add(generationRequest);
        }

        return requests;
    }

    private List<TaskRun> dummyTaskRuns(int max, boolean useRandomCount) {
        List<TaskRun> runs = new ArrayList<>();
        int runCount;

        if (useRandomCount) {
            if (max <= 0) {
                throw new IllegalArgumentException("Max must be a positive number for random generation.");
            }
            runCount = new Random().nextInt(max) + 1;
        } else {
            runCount = max;
        }

        for (int i = 0; i < runCount; i++) {
            runs.add(dummyTaskRun(i));
        }

        return runs;
    }

    private TaskRun dummyTaskRun(int index) {
        return new TaskRunBuilder().withNewMetadata()
                .withName("generation-task-run-" + index)
                .withLabels(Map.of(Labels.LABEL_PHASE, SbomGenerationPhase.GENERATE.name().toLowerCase()))
                .endMetadata()
                .withNewSpec()
                .withParams(
                        new ParamBuilder().withName("index").withValue(new ParamValue(String.valueOf(index))).build())
                .endSpec()
                .build();
    }

    @SuppressWarnings("unchecked")
    private Context<GenerationRequest> mockContext(Set<TaskRun> secondaryResources) {
        Context<GenerationRequest> mockedContext = Mockito.mock(Context.class);

        when(mockedContext.getSecondaryResources(TaskRun.class)).thenReturn(secondaryResources);

        return mockedContext;
    }

    private void setTaskrunStatus(TaskRun taskRun, String status) {
        taskRun.setStatus(
                new TaskRunStatusBuilder().withConditions(List.of(new ConditionBuilder().withStatus(status).build()))
                        .build());
    }

    private List<UpdateControl<GenerationRequest>> executeReconcileTasksAndGetResults(
            Path tmpDir,
            int totalRequests,
            int concurrencyLimit,
            long slowReadDelayMillis) throws Exception {

        List<GenerationRequest> requests = generationRequests(totalRequests, false, tmpDir);
        executorService = Executors.newFixedThreadPool(totalRequests);

        try (MockedStatic<SbomUtils> mockedSbomUtils = mockStatic(SbomUtils.class)) {
            mockedSbomUtils.when(() -> SbomUtils.fromPath(any(Path.class))).thenAnswer(invocation -> {
                Thread.sleep(slowReadDelayMillis);
                return invocation.callRealMethod();
            });

            Instant startTime = Instant.now();

            List<Future<UpdateControl<GenerationRequest>>> futures = new ArrayList<>();
            for (GenerationRequest request : requests) {
                Set<TaskRun> secondaryTaskRuns = new LinkedHashSet<>(dummyTaskRuns(3, false));
                secondaryTaskRuns.forEach(tr -> setTaskrunStatus(tr, "True"));
                Context<GenerationRequest> context = mockContext(secondaryTaskRuns);

                Future<UpdateControl<GenerationRequest>> future = executorService
                        .submit(() -> sic.reconcile(request, context));
                futures.add(future);
            }

            executorService.shutdown();
            assertTrue(executorService.awaitTermination(60, TimeUnit.SECONDS));

            Instant endTime = Instant.now();
            long durationMillis = Duration.between(startTime, endTime).toMillis();
            long expectedMinimumDuration = (long) Math.ceil((double) totalRequests / concurrencyLimit)
                    * slowReadDelayMillis;

            assertTrue(
                    durationMillis >= expectedMinimumDuration,
                    "Total execution time should be at least " + expectedMinimumDuration
                            + "ms due to semaphore throttling.");

            List<UpdateControl<GenerationRequest>> results = new ArrayList<>();
            try {
                for (Future<UpdateControl<GenerationRequest>> future : futures) {
                    UpdateControl<GenerationRequest> updateControl = future.get();
                    results.add(updateControl);
                }
            } catch (Exception e) {
                fail("One of the reconcile tasks threw an unhandled exception", e);
            }
            return results;
        }
    }

    @Test
    void testConcurrencyLimitOnReadManifests(@TempDir Path tmpDir) throws Exception {
        // Dont complain about atlas upload failing
        doNothing().when(atlasHandler).publishBuildManifests(any());

        int totalRequests = 5;
        int concurrencyLimit = 2; // Only used to calculate expected result
        /*
         * Too high and we will exceed the 30 second timeout, too low and we lessen the chance that we will encounter
         * multiple concurrent ops
         */
        long slowReadDelayMillis = 200;

        List<UpdateControl<GenerationRequest>> results = executeReconcileTasksAndGetResults(
                tmpDir,
                totalRequests,
                concurrencyLimit,
                slowReadDelayMillis);

        for (UpdateControl<GenerationRequest> result : results) {
            assertNotNull(result, "The returned UpdateControl should not be null");
            assertEquals(SbomGenerationStatus.FINISHED, result.getResource().get().getStatus(), "Expected FINISHED");
        }

        List<UpdateControl<GenerationRequest>> failed = results.stream()
                .filter(r -> r.getResource().get().getStatus().equals(SbomGenerationStatus.FAILED))
                .collect(Collectors.toList());

        assertTrue(failed.size() < 1, "We shouldn't get any generation failures");
        assertEquals(totalRequests, results.size(), "A result for every request submitted");
    }

    @Test
    void testMaxRetriesExceeded(@TempDir Path tmpDir) throws Exception {
        int totalRequests = 10; // We set this to 10 here to ensure we exceed bulkhead and our retries
        int concurrencyLimit = 2;
        long slowReadDelayMillis = 200;

        List<UpdateControl<GenerationRequest>> results = executeReconcileTasksAndGetResults(
                tmpDir,
                totalRequests,
                concurrencyLimit,
                slowReadDelayMillis);

        for (UpdateControl<GenerationRequest> result : results) {
            assertNotNull(result, "The returned UpdateControl should not be null");
        }

        // We want number of failed generations in results and the reason given contains bulkhead exception
        List<UpdateControl<GenerationRequest>> failed = results.stream()
                .filter(r -> r.getResource().get().getStatus().equals(SbomGenerationStatus.FAILED))
                .collect(Collectors.toList());

        List<String> failedReasons = failed.stream().map(fr -> fr.getResource().get().getReason()).distinct().toList();

        assertTrue(failed.size() > 0, "We should get atleast one generation failure");
        assertTrue(
                failedReasons.stream().anyMatch(reason -> reason.contains("Boms rejected from bulkhead")),
                "We where expecting to fail generation with a bulkhead exception");
        assertEquals(totalRequests, results.size(), "A result for every request submitted");
    }
}
