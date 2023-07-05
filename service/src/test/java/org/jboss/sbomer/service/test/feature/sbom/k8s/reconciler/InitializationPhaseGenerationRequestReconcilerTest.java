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
package org.jboss.sbomer.service.test.feature.sbom.k8s.reconciler;

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.jboss.sbomer.core.features.sbom.enums.GenerationResult;
import org.jboss.sbomer.core.test.TestResources;
import org.jboss.sbomer.service.feature.sbom.k8s.model.GenerationRequest;
import org.jboss.sbomer.service.feature.sbom.k8s.model.GenerationRequestBuilder;
import org.jboss.sbomer.service.feature.sbom.k8s.model.SbomGenerationPhase;
import org.jboss.sbomer.service.feature.sbom.k8s.model.SbomGenerationStatus;
import org.jboss.sbomer.service.feature.sbom.k8s.reconciler.GenerationRequestReconciler;
import org.jboss.sbomer.service.feature.sbom.k8s.resources.Labels;
import org.jboss.sbomer.service.feature.sbom.k8s.resources.TaskRunInitDependentResource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;

import io.fabric8.knative.internal.pkg.apis.ConditionBuilder;
import io.fabric8.kubernetes.api.model.ContainerStateTerminatedBuilder;
import io.fabric8.tekton.pipeline.v1beta1.ArrayOrString;
import io.fabric8.tekton.pipeline.v1beta1.ParamBuilder;
import io.fabric8.tekton.pipeline.v1beta1.StepStateBuilder;
import io.fabric8.tekton.pipeline.v1beta1.TaskRun;
import io.fabric8.tekton.pipeline.v1beta1.TaskRunBuilder;
import io.fabric8.tekton.pipeline.v1beta1.TaskRunResultBuilder;
import io.fabric8.tekton.pipeline.v1beta1.TaskRunStatusBuilder;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;
import io.quarkus.test.junit.QuarkusTest;

/**
 * Class responsible for testing reconciliation workflow for the initialization phase.
 */
@QuarkusTest
public class InitializationPhaseGenerationRequestReconcilerTest {

    @Inject
    GenerationRequestReconciler controller;

    private GenerationRequest dummyInitializationRequest() throws IOException {
        return new GenerationRequestBuilder().withNewMetadata()
                .withName("test")
                .endMetadata()
                .withBuildId("AABBCC")
                .withStatus(SbomGenerationStatus.INITIALIZING)
                .build();
    }

    private TaskRun dummyTaskRun() {
        return new TaskRunBuilder().withNewMetadata()
                .withName("init-task-run")
                .withLabels(Map.of(Labels.LABEL_PHASE, SbomGenerationPhase.INIT.name().toLowerCase()))
                .endMetadata()
                .withNewSpec()
                .withParams(new ParamBuilder().withName("build-id").withValue(new ArrayOrString("AABBCC")).build())
                .endSpec()
                .build();
    }

    private void setTaskRunStatus(TaskRun taskRun, String status) {
        taskRun.setStatus(
                new TaskRunStatusBuilder().withConditions(List.of(new ConditionBuilder().withStatus(status).build()))
                        .build());
    }

    private void setTaskRunResult(TaskRun taskRun) throws IOException {
        taskRun.getStatus()
                .getTaskResults()
                .add(
                        new TaskRunResultBuilder().withName(TaskRunInitDependentResource.RESULT_NAME)
                                .withValue(new ArrayOrString(TestResources.asString("configs/multi-product.yaml")))
                                .build());
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

    @Test
    void testMissingTaskRun() throws Exception {
        GenerationRequest request = dummyInitializationRequest();

        UpdateControl<GenerationRequest> updateControl = controller
                .reconcile(request, mockContext(Collections.emptySet()));

        assertTrue(updateControl.isUpdateResource());
        assertEquals(SbomGenerationStatus.FAILED, updateControl.getResource().getStatus());
        assertEquals(
                "Configuration initialization failed. Unable to find related TaskRun. See logs for more information.",
                updateControl.getResource().getReason());
        assertEquals(GenerationResult.ERR_SYSTEM, updateControl.getResource().getResult());
    }

    @Test
    void testSuccessful() throws Exception {
        GenerationRequest request = dummyInitializationRequest();

        TaskRun taskRun = dummyTaskRun();

        // Set the Task run status to reflect it being in "finished" state
        setTaskRunStatus(taskRun, "True");
        setTaskRunResult(taskRun);

        UpdateControl<GenerationRequest> updateControl = controller.reconcile(request, mockContext(Set.of(taskRun)));

        assertTrue(updateControl.isUpdateResource());
        assertEquals(SbomGenerationStatus.INITIALIZED, updateControl.getResource().getStatus());

        // For in-progress generation we don't ser reason nor result
        assertNull(updateControl.getResource().getReason());
        assertNull(updateControl.getResource().getResult());
    }

    @Test
    void testInProgress() throws Exception {
        GenerationRequest request = dummyInitializationRequest();

        TaskRun taskRun = dummyTaskRun();

        // Set the Task run status to reflect it being in "running" state
        setTaskRunStatus(taskRun, "Unknown");

        UpdateControl<GenerationRequest> updateControl = controller.reconcile(request, mockContext(Set.of(taskRun)));

        assertTrue(updateControl.isNoUpdate());
    }

    @ParameterizedTest
    @CsvSource({
            "1, ERR_GENERAL, Configuration initialization failed. General error occurred. See logs for more information.",
            "2, ERR_CONFIG_INVALID, Configuration initialization failed. Configuration validation failed. See logs for more information.",
            "3, ERR_CONFIG_MISSING, Configuration initialization failed. Could not find configuration. See logs for more information.",
            "99, ERR_SYSTEM, Configuration initialization failed. System error occurred. See logs for more information." })
    void testFailed(int exitCode, GenerationResult result, String reason) throws Exception {
        GenerationRequest request = dummyInitializationRequest();

        TaskRun taskRun = dummyTaskRun();

        setTaskRunStatus(taskRun, "False");
        setTaskRunExitCode(taskRun, exitCode);

        UpdateControl<GenerationRequest> updateControl = controller.reconcile(request, mockContext(Set.of(taskRun)));

        assertTrue(updateControl.isUpdateResource());
        assertEquals(SbomGenerationStatus.FAILED, updateControl.getResource().getStatus());
        assertEquals(reason, updateControl.getResource().getReason());
        assertEquals(result, updateControl.getResource().getResult());
    }
}
