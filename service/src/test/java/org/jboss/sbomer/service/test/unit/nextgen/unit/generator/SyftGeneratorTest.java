package org.jboss.sbomer.service.test.unit.nextgen.unit.generator;

import static org.jboss.sbomer.service.nextgen.controller.tekton.AbstractTektonController.IS_OOM_KILLED;
import static org.jboss.sbomer.service.nextgen.generator.syft.SyftGenerator.ANNOTATION_RETRY_COUNT;
import static org.jboss.sbomer.service.nextgen.generator.syft.SyftGenerator.CPU_OVERRIDE;
import static org.jboss.sbomer.service.nextgen.generator.syft.SyftGenerator.GENERATE_OVERRIDE;
import static org.jboss.sbomer.service.nextgen.generator.syft.SyftGenerator.MEMORY_OVERRIDE;
import static org.jboss.sbomer.service.nextgen.generator.syft.SyftGenerator.PARAM_COMMAND_CONTAINER_IMAGE;
import static org.jboss.sbomer.service.nextgen.generator.syft.SyftGenerator.SA_SUFFIX;
import static org.jboss.sbomer.service.nextgen.generator.syft.SyftGenerator.SERVICE_SUFFIX;
import static org.jboss.sbomer.service.nextgen.generator.syft.SyftGenerator.TASK_SUFFIX;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.text.ParseException;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.eclipse.microprofile.context.ManagedExecutor;
import org.jboss.sbomer.core.features.sbom.utils.ObjectMapperProvider;
import org.jboss.sbomer.service.feature.sbom.config.GenerationRequestControllerConfig;
import org.jboss.sbomer.service.feature.sbom.k8s.model.SbomGenerationPhase;
import org.jboss.sbomer.service.leader.LeaderManager;
import org.jboss.sbomer.service.nextgen.controller.tekton.AbstractTektonController;
import org.jboss.sbomer.service.nextgen.core.dto.api.GenerationRequest;
import org.jboss.sbomer.service.nextgen.core.dto.api.Generator;
import org.jboss.sbomer.service.nextgen.core.dto.api.GeneratorConfig;
import org.jboss.sbomer.service.nextgen.core.dto.api.Target;
import org.jboss.sbomer.service.nextgen.core.dto.model.GenerationRecord;
import org.jboss.sbomer.service.nextgen.core.enums.GenerationResult;
import org.jboss.sbomer.service.nextgen.core.enums.GenerationStatus;
import org.jboss.sbomer.service.nextgen.core.payloads.generation.GenerationStatusUpdatePayload;
import org.jboss.sbomer.service.nextgen.core.rest.SBOMerClient;
import org.jboss.sbomer.service.nextgen.core.utils.JacksonUtils;
import org.jboss.sbomer.service.nextgen.generator.GeneratorRetries;
import org.jboss.sbomer.service.nextgen.generator.syft.SyftContainerImageOptions;
import org.jboss.sbomer.service.nextgen.generator.syft.SyftGenerator;
import org.jboss.sbomer.service.nextgen.service.EntityMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import io.fabric8.knative.pkg.apis.Condition;
import io.fabric8.kubernetes.api.model.ContainerStateTerminated;
import io.fabric8.kubernetes.api.model.Duration;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.OwnerReferenceBuilder;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaimVolumeSourceBuilder;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.api.model.apps.DeploymentList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.dsl.AppsAPIGroupDSL;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.dsl.RollableScalableResource;
import io.fabric8.tekton.v1beta1.ParamBuilder;
import io.fabric8.tekton.v1beta1.StepState;
import io.fabric8.tekton.v1beta1.TaskRefBuilder;
import io.fabric8.tekton.v1beta1.TaskRun;
import io.fabric8.tekton.v1beta1.TaskRunBuilder;
import io.fabric8.tekton.v1beta1.TaskRunStatus;
import io.fabric8.tekton.v1beta1.TaskRunStepOverrideBuilder;
import io.fabric8.tekton.v1beta1.WorkspaceBindingBuilder;
import jakarta.enterprise.inject.Vetoed;

public class SyftGeneratorTest {

    private static final String IDENTIFIER = "quay.io/org/image1:tag";
    private static final String RELEASE = "sbomer";
    private static final String DEFAULT_REQUESTS_MEMORY = "768Mi";
    private static final String DEFAULT_LIMITS_MEMORY = "1536Mi";

    @Vetoed
    static class SyftGeneratorAlt extends SyftGenerator {
        public SyftGeneratorAlt(
                SBOMerClient sbomerClient,
                KubernetesClient kubernetesClient,
                GenerationRequestControllerConfig controllerConfig,
                ManagedExecutor managedExecutor,
                EntityMapper mapper,
                LeaderManager leaderManager) {
            super(sbomerClient, kubernetesClient, controllerConfig, managedExecutor, mapper, leaderManager);
        }

        @Override
        public void reconcileGenerating(GenerationRecord generation, Set<TaskRun> relatedTaskRuns) {
            super.reconcileGenerating(generation, relatedTaskRuns);
        }
    }

    SBOMerClient sbomerClientMock;
    KubernetesClient kubernetesClientMock;
    SyftGeneratorAlt syftGenerator;
    Map<String, GenerationRecord> generationRecordsById;
    AppsAPIGroupDSL appsMock;
    MixedOperation<Deployment, DeploymentList, RollableScalableResource<Deployment>> deploymentsMock;
    RollableScalableResource<Deployment> resourceMock;
    MixedOperation<TaskRun, KubernetesResourceList<TaskRun>, Resource<TaskRun>> taskRunsMock;

    @BeforeEach
    void beforeEach() {
        kubernetesClientMock = mock(KubernetesClient.class);
        appsMock = mock(AppsAPIGroupDSL.class);
        deploymentsMock = mock(MixedOperation.class);
        resourceMock = mock(RollableScalableResource.class);
        Deployment deployment = new DeploymentBuilder()
                .withMetadata(new ObjectMetaBuilder().withUid(UUID.randomUUID().toString()).build())
                .build();
        taskRunsMock = mock(MixedOperation.class);
        sbomerClientMock = mock(SBOMerClient.class);
        syftGenerator = new SyftGeneratorAlt(sbomerClientMock, kubernetesClientMock, null, null, null, null);
        generationRecordsById = new HashMap<>();

        // Simulate update of generation record
        when(sbomerClientMock.updateGenerationStatus(any(), any())).thenAnswer(invocation -> {
            String generationId = invocation.getArgument(0);
            GenerationStatusUpdatePayload payload = invocation.getArgument(1);
            GenerationRecord existingGenerationRecord = generationRecordsById.get(generationId);
            GenerationRecord updatedGenerationRecord = new GenerationRecord(
                    generationId,
                    existingGenerationRecord.created(),
                    existingGenerationRecord.updated(),
                    existingGenerationRecord.finished(),
                    existingGenerationRecord.request(),
                    existingGenerationRecord.metadata(),
                    payload.status(),
                    payload.result(),
                    payload.reason());
            generationRecordsById.put(generationId, updatedGenerationRecord);
            return updatedGenerationRecord;
        });
        when(kubernetesClientMock.apps()).thenReturn(appsMock);
        when(appsMock.deployments()).thenReturn(deploymentsMock);
        when(deploymentsMock.withName(RELEASE + SERVICE_SUFFIX)).thenReturn(resourceMock);
        when(resourceMock.get()).thenReturn(deployment);
        when(kubernetesClientMock.resources(TaskRun.class)).thenReturn(taskRunsMock);
    }

    @Test
    void testFailedTask() throws ParseException {
        GenerationRecord generationRecord = createGenerationRecord();
        TaskRun taskRun = createTaskRun(generationRecord, "0", DEFAULT_REQUESTS_MEMORY, DEFAULT_LIMITS_MEMORY);
        addTaskRunStatus(taskRun, 1, "Error");

        syftGenerator.reconcileGenerating(generationRecord, Set.of(taskRun));

        generationRecord = generationRecordsById.get(generationRecord.id());
        assertEquals(GenerationStatus.FAILED, generationRecord.status());
        assertEquals(GenerationResult.ERR_GENERAL, generationRecord.result());
        StepState stepState = taskRun.getStatus().getSteps().get(0);
        ContainerStateTerminated terminated = stepState.getTerminated();
        assertEquals(
                "Generation failed, the TaskRun returned failure: Step '" + stepState.getName() + "' failed: exitCode="
                        + terminated.getExitCode() + " (Exited with code " + terminated.getExitCode() + "), reason="
                        + terminated.getReason(),
                generationRecord.reason());
    }

    @Test
    void testOomTask() throws ParseException {
        GenerationRecord generationRecord = createGenerationRecord();
        TaskRun taskRun = createTaskRun(generationRecord, "0", DEFAULT_REQUESTS_MEMORY, DEFAULT_LIMITS_MEMORY);
        addTaskRunStatus(taskRun, 137, IS_OOM_KILLED);

        // Simulate delete and create of task runs
        Resource<TaskRun> resourceMock = mock(Resource.class);
        when(taskRunsMock.resource(any())).thenReturn(resourceMock);
        when(resourceMock.delete()).thenReturn(List.of());
        when(resourceMock.create()).thenReturn(null);

        syftGenerator.reconcileGenerating(generationRecord, Set.of(taskRun));

        ArgumentCaptor<TaskRun> taskRunCaptor = ArgumentCaptor.forClass(TaskRun.class);
        verify(taskRunsMock, atLeast(2)).resource(taskRunCaptor.capture());
        List<TaskRun> taskRuns = taskRunCaptor.getAllValues();
        TaskRun updatedTaskRun = taskRuns.get(1);
        String updatedRetryCount = "1";

        assertEquals(2, taskRuns.size());
        assertEquals(taskRun, taskRuns.get(0));
        assertEquals(
                taskRun.getMetadata().getName() + "-retry-" + updatedRetryCount,
                updatedTaskRun.getMetadata().getName());
        assertEquals(updatedRetryCount, updatedTaskRun.getMetadata().getAnnotations().get(ANNOTATION_RETRY_COUNT));
        assertEquals(
                new Quantity("999Mi"),
                updatedTaskRun.getSpec().getStepOverrides().get(0).getResources().getRequests().get(MEMORY_OVERRIDE));
        assertEquals(
                new Quantity("1997Mi"),
                updatedTaskRun.getSpec().getStepOverrides().get(0).getResources().getLimits().get(MEMORY_OVERRIDE));
    }

    @Test
    void testOomTaskSecondRetry() throws ParseException {
        GenerationRecord generationRecord = createGenerationRecord();
        TaskRun taskRun = createTaskRun(generationRecord, "1", "999Mi", "1997Mi");
        addTaskRunStatus(taskRun, 137, IS_OOM_KILLED);

        // Simulate delete and create of task runs
        Resource<TaskRun> resourceMock = mock(Resource.class);
        when(taskRunsMock.resource(any())).thenReturn(resourceMock);
        when(resourceMock.delete()).thenReturn(List.of());
        when(resourceMock.create()).thenReturn(null);

        syftGenerator.reconcileGenerating(generationRecord, Set.of(taskRun));

        ArgumentCaptor<TaskRun> taskRunCaptor = ArgumentCaptor.forClass(TaskRun.class);
        verify(taskRunsMock, atLeast(2)).resource(taskRunCaptor.capture());
        List<TaskRun> taskRuns = taskRunCaptor.getAllValues();
        TaskRun updatedTaskRun = taskRuns.get(1);
        String updatedRetryCount = "2";

        assertEquals(2, taskRuns.size());
        assertEquals(taskRun, taskRuns.get(0));
        assertEquals(
                taskRun.getMetadata().getName() + "-retry-" + updatedRetryCount,
                updatedTaskRun.getMetadata().getName());
        assertEquals(updatedRetryCount, updatedTaskRun.getMetadata().getAnnotations().get(ANNOTATION_RETRY_COUNT));
        assertEquals(
                new Quantity("1299Mi"),
                updatedTaskRun.getSpec().getStepOverrides().get(0).getResources().getRequests().get(MEMORY_OVERRIDE));
        assertEquals(
                new Quantity("2597Mi"),
                updatedTaskRun.getSpec().getStepOverrides().get(0).getResources().getLimits().get(MEMORY_OVERRIDE));
    }

    @Test
    void testOomTaskExhaustedRetries() throws ParseException {
        GenerationRecord generationRecord = createGenerationRecord();
        TaskRun taskRun = createTaskRun(generationRecord, "3", "1689Mi", "3377Mi");
        addTaskRunStatus(taskRun, 137, IS_OOM_KILLED);

        // Simulate delete and create of task runs
        Resource<TaskRun> resourceMock = mock(Resource.class);
        when(taskRunsMock.resource(any())).thenReturn(resourceMock);
        when(resourceMock.delete()).thenReturn(List.of());
        when(resourceMock.create()).thenReturn(null);

        syftGenerator.reconcileGenerating(generationRecord, Set.of(taskRun));

        generationRecord = generationRecordsById.get(generationRecord.id());
        assertEquals(GenerationStatus.FAILED, generationRecord.status());
        assertEquals(GenerationResult.ERR_OOM, generationRecord.result());
        StepState stepState = taskRun.getStatus().getSteps().get(0);
        ContainerStateTerminated terminated = stepState.getTerminated();
        assertEquals(
                "Generation failed, the TaskRun returned failure: Step '" + stepState.getName() + "' failed: exitCode="
                        + terminated.getExitCode()
                        + " (Terminated by signal 9 (Killed: forcefully terminated, likely OOMKilled)), reason="
                        + terminated.getReason(),
                generationRecord.reason());
    }

    @Test
    void testOomTaskRetryScheduleFailure() throws ParseException {
        GenerationRecord generationRecord = createGenerationRecord();
        TaskRun taskRun = createTaskRun(generationRecord, "0", DEFAULT_REQUESTS_MEMORY, DEFAULT_LIMITS_MEMORY);
        addTaskRunStatus(taskRun, 137, IS_OOM_KILLED);

        // Simulate delete and create of task runs
        Resource<TaskRun> resourceMock = mock(Resource.class);
        when(taskRunsMock.resource(any())).thenReturn(resourceMock);
        when(resourceMock.delete()).thenReturn(List.of());
        when(resourceMock.create()).thenThrow(new KubernetesClientException("foo"));

        syftGenerator.reconcileGenerating(generationRecord, Set.of(taskRun));

        generationRecord = generationRecordsById.get(generationRecord.id());
        assertEquals(GenerationStatus.FAILED, generationRecord.status());
        assertEquals(GenerationResult.ERR_SYSTEM, generationRecord.result());
        assertEquals("Unable to schedule Tekton TaskRun retry: foo", generationRecord.reason());
    }

    GenerationRecord createGenerationRecord() {
        GenerationRecord generationRecord = new GenerationRecord(
                UUID.randomUUID().toString(),
                Instant.now(),
                Instant.now(),
                null,
                JacksonUtils.toObjectNode(
                        new GenerationRequest(
                                new Generator(
                                        "syft",
                                        "1.0",
                                        new GeneratorConfig(
                                                null,
                                                null,
                                                ObjectMapperProvider.json()
                                                        .valueToTree(
                                                                new SyftContainerImageOptions(
                                                                        false,
                                                                        null,
                                                                        "6h",
                                                                        new GeneratorRetries(3, 1.3d))))),
                                new Target("CONTAINER_IMAGE", IDENTIFIER))),
                null,
                GenerationStatus.GENERATING,
                null,
                null);
        generationRecordsById.put(generationRecord.id(), generationRecord);
        return generationRecord;
    }

    TaskRun createTaskRun(
            GenerationRecord generationRecord,
            String retryCount,
            String requestsMemory,
            String limitsMemory) throws ParseException {
        TaskRun taskRun = new TaskRunBuilder().withNewMetadata()
                .withLabels(
                        Map.of(
                                AbstractTektonController.GENERATION_ID_LABEL,
                                generationRecord.id(),
                                AbstractTektonController.GENERATOR_TYPE,
                                "syft"))
                .withName(
                        "generation-" + generationRecord.id().toLowerCase() + "-"
                                + SbomGenerationPhase.GENERATE.ordinal() + "-"
                                + SbomGenerationPhase.GENERATE.name().toLowerCase())
                .addToAnnotations(ANNOTATION_RETRY_COUNT, retryCount)
                .endMetadata()
                .withNewSpec()
                .withServiceAccountName(RELEASE + SA_SUFFIX)
                .withTimeout(Duration.parse("6h"))
                .withParams(
                        List.of(
                                new ParamBuilder().withName(PARAM_COMMAND_CONTAINER_IMAGE)
                                        .withNewValue(IDENTIFIER)
                                        .build()))
                .withTaskRef(new TaskRefBuilder().withName(RELEASE + TASK_SUFFIX).build())
                .withStepOverrides(
                        new TaskRunStepOverrideBuilder().withName(GENERATE_OVERRIDE)
                                .withNewResources()
                                .withRequests(
                                        Map.of(
                                                CPU_OVERRIDE,
                                                new Quantity("400m"),
                                                MEMORY_OVERRIDE,
                                                new Quantity(requestsMemory)))
                                .withLimits(
                                        Map.of(
                                                CPU_OVERRIDE,
                                                new Quantity("800m"),
                                                MEMORY_OVERRIDE,
                                                new Quantity(limitsMemory)))
                                .endResources()
                                .build())
                .withWorkspaces(
                        new WorkspaceBindingBuilder().withSubPath(generationRecord.id())
                                .withName("data")
                                .withPersistentVolumeClaim(
                                        new PersistentVolumeClaimVolumeSourceBuilder().withClaimName(RELEASE + "-sboms")
                                                .build())
                                .build())
                .endSpec()
                .build();
        taskRun.getMetadata()
                .setOwnerReferences(
                        Collections.singletonList(
                                new OwnerReferenceBuilder().withKind(HasMetadata.getKind(Deployment.class))
                                        .withApiVersion(HasMetadata.getApiVersion(Deployment.class))
                                        .withName(RELEASE + SERVICE_SUFFIX)
                                        .withUid(UUID.randomUUID().toString())
                                        .build()));
        return taskRun;
    }

    void addTaskRunStatus(TaskRun taskRun, int exitCode, String reason) {
        TaskRunStatus taskRunStatus = new TaskRunStatus();
        taskRunStatus.setPodName(taskRun.getMetadata().getName() + "-pod");
        StepState stepState = new StepState();
        stepState.setName("step-" + SbomGenerationPhase.GENERATE.name().toLowerCase());
        ContainerStateTerminated terminated = new ContainerStateTerminated();
        terminated.setExitCode(exitCode);
        terminated.setReason(reason);
        stepState.setTerminated(terminated);
        Condition condition = new Condition(
                null,
                "Step " + stepState.getName() + " failed: container terminated with exit code "
                        + terminated.getExitCode(),
                "Failed",
                null,
                "False",
                "Succeeded");
        taskRunStatus.setConditions(List.of(condition));
        taskRunStatus.setSteps(List.of(stepState));
        taskRun.setStatus(taskRunStatus);
    }

}
