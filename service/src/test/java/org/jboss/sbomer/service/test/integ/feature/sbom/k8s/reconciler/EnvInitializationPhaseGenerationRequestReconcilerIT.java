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

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.inject.Inject;

import org.jboss.sbomer.core.features.sbom.enums.GenerationResult;
import org.jboss.sbomer.core.test.TestResources;
import org.jboss.sbomer.service.feature.sbom.k8s.model.GenerationRequest;
import org.jboss.sbomer.service.feature.sbom.k8s.model.GenerationRequestBuilder;
import org.jboss.sbomer.service.feature.sbom.k8s.model.SbomGenerationPhase;
import org.jboss.sbomer.service.feature.sbom.k8s.model.SbomGenerationStatus;
import org.jboss.sbomer.service.feature.sbom.k8s.reconciler.GenerationRequestReconciler;
import org.jboss.sbomer.service.feature.sbom.k8s.resources.Labels;
import org.jboss.sbomer.service.feature.sbom.k8s.resources.TaskRunEnvDetectDependentResource;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.fabric8.knative.internal.pkg.apis.ConditionBuilder;
import io.fabric8.kubernetes.api.model.ContainerStateTerminatedBuilder;
import io.fabric8.tekton.pipeline.v1beta1.ParamBuilder;
import io.fabric8.tekton.pipeline.v1beta1.ParamValue;
import io.fabric8.tekton.pipeline.v1beta1.StepStateBuilder;
import io.fabric8.tekton.pipeline.v1beta1.TaskRun;
import io.fabric8.tekton.pipeline.v1beta1.TaskRunBuilder;
import io.fabric8.tekton.pipeline.v1beta1.TaskRunResultBuilder;
import io.fabric8.tekton.pipeline.v1beta1.TaskRunStatusBuilder;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;
import io.quarkus.test.junit.QuarkusTest;

/**
 * Class responsible for testing reconciliation workflow for the environment initialization phase.
 */
@QuarkusTest
public class EnvInitializationPhaseGenerationRequestReconcilerIT {

    @Inject
    GenerationRequestReconciler controller;

    private GenerationRequest dummyGenerationRequest(SbomGenerationStatus status) throws IOException {
        return new GenerationRequestBuilder().withNewMetadata()
                .withName("test")
                .endMetadata()
                .withBuildId("AABBCC")
                .withStatus(status)
                .build();
    }

    private TaskRun dummyTaskRun() {
        return new TaskRunBuilder().withNewMetadata()
                .withName("env-init-task-run")
                .withLabels(Map.of(Labels.LABEL_PHASE, SbomGenerationPhase.DETECTENVINFO.name().toLowerCase()))
                .endMetadata()
                .withNewSpec()
                .withParams(new ParamBuilder().withName("build-id").withValue(new ParamValue("AABBCC")).build())
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
                        new TaskRunResultBuilder().withName(TaskRunEnvDetectDependentResource.RESULT_NAME)
                                .withValue(new ParamValue(TestResources.asString("configs/env-config.yaml")))
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
    void testEnvDetectingWithoutDependentResources() throws Exception {
        GenerationRequest request = dummyGenerationRequest(SbomGenerationStatus.ENV_DETECTING);

        UpdateControl<GenerationRequest> updateControl = controller
                .reconcile(request, mockContext(Collections.emptySet()));

        assertTrue(updateControl.isUpdateResource());
        assertEquals(SbomGenerationStatus.FAILED, updateControl.getResource().getStatus());
        assertEquals(
                "Environment detection failed. Unable to find related TaskRun. See logs for more information.",
                updateControl.getResource().getReason());
        assertEquals(GenerationResult.ERR_SYSTEM, updateControl.getResource().getResult());
        assertEquals("FAILED", updateControl.getResource().getMetadata().getLabels().get(Labels.LABEL_STATUS));
        assertEquals("detectenvinfo", updateControl.getResource().getMetadata().getLabels().get(Labels.LABEL_PHASE));
    }

    @Test
    void testSuccessful() throws Exception {
        GenerationRequest request = dummyGenerationRequest(SbomGenerationStatus.ENV_DETECTING);

        TaskRun taskRun = dummyTaskRun();

        // Set the Task run status to reflect it being in "finished" state
        setTaskRunStatus(taskRun, "True");
        setTaskRunResult(taskRun);

        UpdateControl<GenerationRequest> updateControl = controller.reconcile(request, mockContext(Set.of(taskRun)));

        assertTrue(updateControl.isUpdateResource());
        assertEquals(SbomGenerationStatus.ENV_DETECTED, updateControl.getResource().getStatus());

        // For in-progress generation we don't set reason nor result
        assertNull(updateControl.getResource().getReason());
        assertNull(updateControl.getResource().getResult());
        assertEquals("ENV_DETECTED", updateControl.getResource().getMetadata().getLabels().get(Labels.LABEL_STATUS));
    }

    @Test
    void testInProgress() throws Exception {
        GenerationRequest request = dummyGenerationRequest(SbomGenerationStatus.ENV_DETECTING);

        TaskRun taskRun = dummyTaskRun();

        // Set the Task run status to reflect it being in "running" state
        setTaskRunStatus(taskRun, "Unknown");

        UpdateControl<GenerationRequest> updateControl = controller.reconcile(request, mockContext(Set.of(taskRun)));

        assertTrue(updateControl.isNoUpdate());
    }

    @Test
    public void testFailedNoPNCBuild() throws Exception {
        GenerationRequest request = dummyGenerationRequest(SbomGenerationStatus.ENV_DETECTING);

        Set<TaskRun> secondaryTaskRuns = new HashSet<>();

        TaskRun taskRun = dummyTaskRun();
        setTaskRunStatus(taskRun, "False");
        setTaskRunExitCode(taskRun, GenerationResult.ERR_GENERAL.getCode());
        secondaryTaskRuns.add(taskRun);

        UpdateControl<GenerationRequest> updateControl = controller.reconcile(request, mockContext(secondaryTaskRuns));

        assertTrue(updateControl.isUpdateResource());
        assertEquals(SbomGenerationStatus.FAILED, updateControl.getResource().getStatus());
        assertEquals(GenerationResult.ERR_GENERAL, updateControl.getResource().getResult());
        assertEquals(
                "Environment detection failed. System failure. General error occurred (could not retrieve the provided PNC buildId).See logs for more information.",
                updateControl.getResource().getReason());
    }

}
