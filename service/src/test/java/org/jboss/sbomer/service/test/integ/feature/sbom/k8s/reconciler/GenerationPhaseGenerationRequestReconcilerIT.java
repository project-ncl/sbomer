/**
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.jboss.sbomer.core.features.sbom.enums.GenerationResult;
import org.jboss.sbomer.core.test.TestResources;
import org.jboss.sbomer.service.feature.sbom.config.GenerationRequestControllerConfig;
import org.jboss.sbomer.service.feature.sbom.k8s.model.GenerationRequest;
import org.jboss.sbomer.service.feature.sbom.k8s.model.GenerationRequestBuilder;
import org.jboss.sbomer.service.feature.sbom.k8s.model.SbomGenerationPhase;
import org.jboss.sbomer.service.feature.sbom.k8s.model.SbomGenerationStatus;
import org.jboss.sbomer.service.feature.sbom.k8s.reconciler.GenerationRequestReconciler;
import org.jboss.sbomer.service.feature.sbom.k8s.resources.Labels;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import io.fabric8.knative.internal.pkg.apis.ConditionBuilder;
import io.fabric8.kubernetes.api.model.ContainerStateTerminatedBuilder;
import io.fabric8.kubernetes.client.server.mock.KubernetesServer;
import io.fabric8.tekton.pipeline.v1beta1.ArrayOrString;
import io.fabric8.tekton.pipeline.v1beta1.ParamBuilder;
import io.fabric8.tekton.pipeline.v1beta1.StepStateBuilder;
import io.fabric8.tekton.pipeline.v1beta1.TaskRun;
import io.fabric8.tekton.pipeline.v1beta1.TaskRunBuilder;
import io.fabric8.tekton.pipeline.v1beta1.TaskRunStatusBuilder;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;
import io.quarkus.test.Mock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.quarkus.test.kubernetes.client.KubernetesTestServer;
import io.quarkus.test.kubernetes.client.WithKubernetesTestServer;

/**
 * Class responsible for testing reconciliation workflow for the generation phase.
 */
@QuarkusTest
@WithKubernetesTestServer
public class GenerationPhaseGenerationRequestReconcilerIT {

    @ApplicationScoped
    @Mock
    public static class MockedGenerationRequestControllerConfig implements GenerationRequestControllerConfig {

        @Override
        public String sbomDir() {
            throw new UnsupportedOperationException("Unimplemented method 'sbomDir'");
        }

        @Override
        public boolean cleanup() {
            throw new UnsupportedOperationException("Unimplemented method 'cleanup'");
        }

    }

    @InjectMock
    GenerationRequestControllerConfig controllerConfig;

    @Inject
    GenerationRequestReconciler controller;

    @KubernetesTestServer
    KubernetesServer mockServer;

    private GenerationRequest dummyGenerationRequest() throws IOException {
        return new GenerationRequestBuilder().withNewMetadata()
                .withName("test-generation-request")
                .endMetadata()
                .withBuildId("AABBCC")
                .withStatus(SbomGenerationStatus.GENERATING)
                .withConfig(TestResources.asString("configs/multi-product.yaml"))
                .build();
    }

    private TaskRun dummyTaskRun() {
        return dummyTaskRun(1);
    }

    private TaskRun dummyTaskRun(int index) {
        return new TaskRunBuilder().withNewMetadata()
                .withName("generation-task-run-" + String.valueOf(index))
                .withLabels(Map.of(Labels.LABEL_PHASE, SbomGenerationPhase.GENERATE.name().toLowerCase()))
                .endMetadata()
                .withNewSpec()
                .withParams(
                        new ParamBuilder().withName("index")
                                .withValue(new ArrayOrString(String.valueOf(index)))
                                .build())
                .endSpec()
                .build();
    }

    private void setTaskRunExitCode(TaskRun taskRun, int exitCode) throws IOException {
        taskRun.getStatus()
                .getSteps()
                .add(
                        new StepStateBuilder()
                                .withTerminated(new ContainerStateTerminatedBuilder().withExitCode(exitCode).build())
                                .build());
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

    @Test
    public void testMissingGenerationTaskRuns() throws Exception {
        GenerationRequest request = dummyGenerationRequest();

        UpdateControl<GenerationRequest> updateControl = controller
                .reconcile(request, mockContext(Collections.emptySet()));

        assertTrue(updateControl.isUpdateResource());
        assertEquals(SbomGenerationStatus.FAILED, updateControl.getResource().getStatus());
        assertEquals(
                "Generation failed. Expected one or more running TaskRun related to generation. None found. See logs for more information.",
                updateControl.getResource().getReason());
        assertEquals(GenerationResult.ERR_SYSTEM, updateControl.getResource().getResult());
    }

    @Test
    public void testSingleTaskRunWithNoStatus() throws Exception {
        GenerationRequest request = dummyGenerationRequest();
        UpdateControl<GenerationRequest> updateControl = controller
                .reconcile(request, mockContext(Set.of(dummyTaskRun())));

        assertTrue(updateControl.isNoUpdate());
    }

    @Test
    public void testSingleInProgressTaskRun() throws Exception {
        GenerationRequest request = dummyGenerationRequest();

        TaskRun taskRun = dummyTaskRun();

        // Set the Task run status to reflect it being in "running" state
        setTaskrunStatus(taskRun, "Unknown");

        UpdateControl<GenerationRequest> updateControl = controller.reconcile(request, mockContext(Set.of(taskRun)));

        assertTrue(updateControl.isNoUpdate());
    }

    @Test
    public void testMultipleInProgressTaskRun() throws Exception {
        GenerationRequest request = dummyGenerationRequest();

        Set<TaskRun> secondaryTaskRuns = new LinkedHashSet<>();

        // Set the Task run status to reflect it being in "running" state
        for (int i = 0; i < 5; i++) {
            TaskRun taskRun = dummyTaskRun(i);
            setTaskrunStatus(taskRun, "Unknown");
            secondaryTaskRuns.add(taskRun);
        }

        UpdateControl<GenerationRequest> updateControl = controller.reconcile(request, mockContext(secondaryTaskRuns));

        assertTrue(updateControl.isNoUpdate());
    }

    /**
     * This tests a case where just one TaskRun failed. We don't update anything at this point.
     *
     * @throws Exception
     */
    @Test
    public void testOneFailedTaskRun() throws Exception {
        GenerationRequest request = dummyGenerationRequest();

        Set<TaskRun> secondaryTaskRuns = new LinkedHashSet<>();
        for (int i = 0; i < 5; i++) {
            TaskRun taskRun = dummyTaskRun();
            setTaskrunStatus(taskRun, "Unknown");
            secondaryTaskRuns.add(taskRun);
        }

        TaskRun taskRun = dummyTaskRun();
        setTaskrunStatus(taskRun, "False");
        secondaryTaskRuns.add(taskRun);

        UpdateControl<GenerationRequest> updateControl = controller.reconcile(request, mockContext(secondaryTaskRuns));

        assertTrue(updateControl.isNoUpdate());
    }

    /**
     * This tests a case where just one TaskRun succeeded. We don't update anything at this point.
     *
     * @throws Exception
     */
    @Test
    public void testOneSucceededTaskRun() throws Exception {
        GenerationRequest request = dummyGenerationRequest();

        Set<TaskRun> secondaryTaskRuns = new LinkedHashSet<>();
        for (int i = 0; i < 5; i++) {
            TaskRun taskRun = dummyTaskRun();
            setTaskrunStatus(taskRun, "Unknown");
            secondaryTaskRuns.add(taskRun);
        }

        TaskRun taskRun = dummyTaskRun();
        setTaskrunStatus(taskRun, "True");
        secondaryTaskRuns.add(taskRun);

        UpdateControl<GenerationRequest> updateControl = controller.reconcile(request, mockContext(secondaryTaskRuns));

        assertTrue(updateControl.isNoUpdate());
    }

    /**
     * This tests a case where the failed TaskRun does not have the exit code set at all.
     *
     * @throws Exception
     */
    @Test
    public void testAllFailedTaskRun() throws Exception {
        GenerationRequest request = dummyGenerationRequest();

        Set<TaskRun> secondaryTaskRuns = new LinkedHashSet<>();

        for (int i = 0; i < 5; i++) {
            TaskRun taskRun = dummyTaskRun(i);
            setTaskrunStatus(taskRun, "False");
            secondaryTaskRuns.add(taskRun);
        }

        UpdateControl<GenerationRequest> updateControl = controller.reconcile(request, mockContext(secondaryTaskRuns));

        assertTrue(updateControl.isUpdateResource());
        assertEquals(SbomGenerationStatus.FAILED, updateControl.getResource().getStatus());
        assertEquals(
                "Generation failed. Product with index '0' (TaskRun 'generation-task-run-0') failed: system failure. Product with index '1' (TaskRun 'generation-task-run-1') failed: system failure. Product with index '2' (TaskRun 'generation-task-run-2') failed: system failure. Product with index '3' (TaskRun 'generation-task-run-3') failed: system failure. Product with index '4' (TaskRun 'generation-task-run-4') failed: system failure. See logs for more information.",
                updateControl.getResource().getReason());
        assertEquals(GenerationResult.ERR_MULTI, updateControl.getResource().getResult());
    }

    /**
     * This tests a case where the TaskRun failed because of product configuration-related issues.
     *
     * @throws Exception
     */
    @Test
    public void testFailedProductConfigTaskRun() throws Exception {
        GenerationRequest request = dummyGenerationRequest();

        Set<TaskRun> secondaryTaskRuns = new HashSet<>();

        TaskRun taskRun = dummyTaskRun();
        setTaskrunStatus(taskRun, "False");
        setTaskRunExitCode(taskRun, GenerationResult.ERR_CONFIG_INVALID.getCode());
        secondaryTaskRuns.add(taskRun);

        UpdateControl<GenerationRequest> updateControl = controller.reconcile(request, mockContext(secondaryTaskRuns));

        assertTrue(updateControl.isUpdateResource());
        assertEquals(SbomGenerationStatus.FAILED, updateControl.getResource().getStatus());
        assertEquals(
                "Generation failed. Product with index '1' (TaskRun 'generation-task-run-1') failed: product configuration failure. See logs for more information.",
                updateControl.getResource().getReason());
        assertEquals(GenerationResult.ERR_CONFIG_INVALID, updateControl.getResource().getResult());
    }

    /**
     * This tests a case where the TaskRun failed because of misconfigured product index.
     *
     * @throws Exception
     */
    @Test
    public void testFailedInvalidIndexTaskRun() throws Exception {
        GenerationRequest request = dummyGenerationRequest();

        Set<TaskRun> secondaryTaskRuns = new HashSet<>();

        TaskRun taskRun = dummyTaskRun(123);
        setTaskrunStatus(taskRun, "False");
        setTaskRunExitCode(taskRun, GenerationResult.ERR_INDEX_INVALID.getCode());
        secondaryTaskRuns.add(taskRun);

        UpdateControl<GenerationRequest> updateControl = controller.reconcile(request, mockContext(secondaryTaskRuns));

        assertTrue(updateControl.isUpdateResource());
        assertEquals(SbomGenerationStatus.FAILED, updateControl.getResource().getStatus());
        assertEquals(
                "Generation failed. Product with index '123' (TaskRun 'generation-task-run-123') failed: invalid product index: 123 (should be between 1 and 2). See logs for more information.",
                updateControl.getResource().getReason());
        assertEquals(GenerationResult.ERR_INDEX_INVALID, updateControl.getResource().getResult());
    }

    /**
     * This tests a case where the TaskRun failed because the generator failed.
     *
     * @throws Exception
     */
    @Test
    public void testFailedWhileGeneratingSbomTaskRun() throws Exception {
        GenerationRequest request = dummyGenerationRequest();

        Set<TaskRun> secondaryTaskRuns = new HashSet<>();

        TaskRun taskRun = dummyTaskRun();
        setTaskrunStatus(taskRun, "False");
        setTaskRunExitCode(taskRun, GenerationResult.ERR_GENERATION.getCode());
        secondaryTaskRuns.add(taskRun);

        UpdateControl<GenerationRequest> updateControl = controller.reconcile(request, mockContext(secondaryTaskRuns));

        assertTrue(updateControl.isUpdateResource());
        assertEquals(SbomGenerationStatus.FAILED, updateControl.getResource().getStatus());
        assertEquals(
                "Generation failed. Product with index '1' (TaskRun 'generation-task-run-1') failed: an error occurred while generating the SBOM. See logs for more information.",
                updateControl.getResource().getReason());
        assertEquals(GenerationResult.ERR_GENERATION, updateControl.getResource().getResult());
    }

    /**
     * This tests a case where the TaskRun failed because of a general error.
     *
     * @throws Exception
     */
    @Test
    public void testFailedGeneralErrorTaskRun() throws Exception {
        GenerationRequest request = dummyGenerationRequest();

        Set<TaskRun> secondaryTaskRuns = new HashSet<>();

        TaskRun taskRun = dummyTaskRun();
        setTaskrunStatus(taskRun, "False");
        setTaskRunExitCode(taskRun, GenerationResult.ERR_GENERAL.getCode());
        secondaryTaskRuns.add(taskRun);

        UpdateControl<GenerationRequest> updateControl = controller.reconcile(request, mockContext(secondaryTaskRuns));

        assertTrue(updateControl.isUpdateResource());
        assertEquals(SbomGenerationStatus.FAILED, updateControl.getResource().getStatus());
        assertEquals(
                "Generation failed. Product with index '1' (TaskRun 'generation-task-run-1') failed: general error occurred. See logs for more information.",
                updateControl.getResource().getReason());
        assertEquals(GenerationResult.ERR_GENERAL, updateControl.getResource().getResult());
    }

    @Test
    public void testFailedMultipleReasons() throws Exception {
        GenerationRequest request = dummyGenerationRequest();

        Set<TaskRun> secondaryTaskRuns = new HashSet<>();

        TaskRun taskRun1 = dummyTaskRun(1);
        setTaskrunStatus(taskRun1, "False");
        setTaskRunExitCode(taskRun1, GenerationResult.ERR_CONFIG_INVALID.getCode());

        TaskRun taskRun2 = dummyTaskRun(2);
        setTaskrunStatus(taskRun2, "False");
        setTaskRunExitCode(taskRun2, GenerationResult.ERR_INDEX_INVALID.getCode());

        secondaryTaskRuns.add(taskRun1);
        secondaryTaskRuns.add(taskRun2);

        UpdateControl<GenerationRequest> updateControl = controller.reconcile(request, mockContext(secondaryTaskRuns));

        assertTrue(updateControl.isUpdateResource());
        assertEquals(SbomGenerationStatus.FAILED, updateControl.getResource().getStatus());
        assertEquals(
                "Generation failed. Product with index '1' (TaskRun 'generation-task-run-1') failed: product configuration failure. Product with index '2' (TaskRun 'generation-task-run-2') failed: invalid product index: 2 (should be between 1 and 2). See logs for more information.",
                updateControl.getResource().getReason());
        assertEquals(GenerationResult.ERR_MULTI, updateControl.getResource().getResult());
    }

    @Test
    public void testFailedWithCleanup(@TempDir Path tempDir) throws Exception {
        when(controllerConfig.cleanup()).thenReturn(true);
        when(controllerConfig.sbomDir()).thenReturn(tempDir.toAbsolutePath().toString());

        // Let's create the expected path for the working directory and some content in it
        Path newDirPath = Files
                .createDirectory(Path.of(tempDir.toAbsolutePath().toString(), "test-generation-request"));
        Path filePath = Path.of(newDirPath.toAbsolutePath().toString(), "file.txt");
        Files.write(filePath, "This is file content".getBytes());

        GenerationRequest request = dummyGenerationRequest();
        request.setStatus(SbomGenerationStatus.FAILED);

        UpdateControl<GenerationRequest> updateControl = controller
                .reconcile(request, mockContext(Collections.emptySet()));

        assertTrue(updateControl.isNoUpdate());

        assertEquals("DELETE", mockServer.getLastRequest().getMethod());
        assertEquals(
                "/api/v1/namespaces/test/configmaps/test-generation-request",
                mockServer.getLastRequest().getPath());

        assertTrue(Files.exists(tempDir));
        assertFalse(Files.exists(filePath));
        assertFalse(Files.exists(newDirPath));
    }

    @Test
    public void testFailedWithoutCleanup(@TempDir Path tempDir) throws Exception {
        when(controllerConfig.cleanup()).thenReturn(false);
        when(controllerConfig.sbomDir()).thenReturn(tempDir.toAbsolutePath().toString());

        // Let's create the expected path for the working directory and some content in it
        Path newDirPath = Files
                .createDirectory(Path.of(tempDir.toAbsolutePath().toString(), "test-generation-request"));
        Path filePath = Path.of(newDirPath.toAbsolutePath().toString(), "file.txt");
        Files.write(filePath, "This is file content".getBytes());

        GenerationRequest request = dummyGenerationRequest();
        request.setStatus(SbomGenerationStatus.FAILED);

        UpdateControl<GenerationRequest> updateControl = controller
                .reconcile(request, mockContext(Collections.emptySet()));

        assertTrue(updateControl.isNoUpdate());
        assertNull(mockServer.getLastRequest());
        assertTrue(Files.exists(tempDir));
        assertTrue(Files.exists(filePath));
        assertTrue(Files.exists(newDirPath));
    }
}
