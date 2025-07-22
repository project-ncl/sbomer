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
package org.jboss.sbomer.service.nextgen.generator.syft;

import java.io.IOException;
import java.nio.file.Path;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.eclipse.microprofile.context.ManagedExecutor;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.sbomer.core.errors.ApplicationException;
import org.jboss.sbomer.core.features.sbom.utils.FileUtils;
import org.jboss.sbomer.core.features.sbom.utils.MDCUtils;
import org.jboss.sbomer.core.features.sbom.utils.ObjectMapperProvider;
import org.jboss.sbomer.service.feature.sbom.config.GenerationRequestControllerConfig;
import org.jboss.sbomer.service.feature.sbom.k8s.model.SbomGenerationPhase;
import org.jboss.sbomer.service.feature.sbom.k8s.resources.Labels;
import org.jboss.sbomer.service.leader.LeaderManager;
import org.jboss.sbomer.service.nextgen.controller.tekton.AbstractTektonController;
import org.jboss.sbomer.service.nextgen.core.dto.api.GenerationRequest;
import org.jboss.sbomer.service.nextgen.core.dto.model.GenerationRecord;
import org.jboss.sbomer.service.nextgen.core.dto.model.ManifestRecord;
import org.jboss.sbomer.service.nextgen.core.enums.GenerationResult;
import org.jboss.sbomer.service.nextgen.core.enums.GenerationStatus;
import org.jboss.sbomer.service.nextgen.core.rest.SBOMerClient;
import org.jboss.sbomer.service.nextgen.core.utils.JacksonUtils;
import org.jboss.sbomer.service.nextgen.service.EntityMapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

import io.fabric8.kubernetes.api.model.Duration;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.OwnerReferenceBuilder;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaimVolumeSourceBuilder;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.ResourceRequirements;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.tekton.v1beta1.Param;
import io.fabric8.tekton.v1beta1.ParamBuilder;
import io.fabric8.tekton.v1beta1.ParamValue;
import io.fabric8.tekton.v1beta1.TaskRefBuilder;
import io.fabric8.tekton.v1beta1.TaskRun;
import io.fabric8.tekton.v1beta1.TaskRunBuilder;
import io.fabric8.tekton.v1beta1.TaskRunSpec;
import io.fabric8.tekton.v1beta1.TaskRunStepOverride;
import io.fabric8.tekton.v1beta1.TaskRunStepOverrideBuilder;
import io.fabric8.tekton.v1beta1.WorkspaceBindingBuilder;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.ValidationException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ApplicationScoped
public class SyftGenerator extends AbstractTektonController {
    public static final String PARAM_COMMAND_CONTAINER_IMAGE = "image";
    public static final String PARAM_COMMAND_TYPE = "type";
    public static final String PARAM_COMMAND_IDENTIFIER = "identifier";
    public static final String PARAM_PATHS = "paths";
    public static final String PARAM_RPMS = "rpms";
    public static final String PARAM_PROCESSORS = "processors";
    public static final String SA_SUFFIX = "-sa";
    public static final String TASK_SUFFIX = "-generator-syft";
    public static final String ANNOTATION_RETRY_COUNT = "sbomer.jboss.org/retry-count";
    public static final String GENERATE_OVERRIDE = "generate";
    public static final String CPU_OVERRIDE = "cpu";
    public static final String MEMORY_OVERRIDE = "memory";
    public static final String SERVICE_SUFFIX = "-service";
    public static final String RETRY_SUFFIX = "-retry-";
    public static final String GENERATOR_NAME = "syft";

    private SyftGenerator() {
        super(null, null, null, null, null, null);
    }

    @Inject
    public SyftGenerator(
            @RestClient SBOMerClient sbomerClient,
            KubernetesClient kubernetesClient,
            GenerationRequestControllerConfig controllerConfig,
            ManagedExecutor managedExecutor,
            EntityMapper mapper,
            LeaderManager leaderManager) {
        super(sbomerClient, kubernetesClient, controllerConfig, managedExecutor, mapper, leaderManager);
    }

    @Override
    public Set<String> getSupportedTypes() {
        return Set.of("CONTAINER_IMAGE");
    }

    @Override
    public String getGeneratorName() {
        return "syft";
    }

    @Override
    public String getGeneratorVersion() {
        return "1.27.1";
    }

    @Override
    public void generate(GenerationRecord generationRecord) {
        log.info("Preparing Tekton Task Run for generation '{}'", generationRecord.id());

        TaskRun desired = desired(generationRecord);

        log.trace("Prepared TaskRun for generation '{}': {}", generationRecord.id(), desired);

        try {
            kubernetesClient.resources(TaskRun.class).resource(desired).create();
        } catch (KubernetesClientException e) {
            log.warn("Unable to schedule Tekton TaskRun", e);

            updateStatus(
                    generationRecord.id(),
                    GenerationStatus.FAILED,
                    GenerationResult.ERR_SYSTEM,
                    "Unable to schedule Tekton TaskRun: {}",
                    e.getMessage());

            return;
        }
    }

    @Scheduled(
            every = "20s",
            delay = 10,
            delayUnit = TimeUnit.SECONDS,
            concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    @Override
    protected void ensureInformer() {
        super.ensureInformer();
    }

    @Override
    protected void reconcileGenerating(GenerationRecord generation, Set<TaskRun> relatedTaskRuns) {
        log.debug("Reconcile '{}' for Generation '{}'...", GenerationStatus.GENERATING, generation.id());

        boolean inProgress = false;
        boolean success = true;
        TaskRun erroredTaskRun = null;

        for (TaskRun tr : relatedTaskRuns) {

            if (!isFinished(tr)) {
                inProgress = true;
                break;
            }

            if (!Boolean.TRUE.equals(isSuccessful(tr))) {
                erroredTaskRun = tr;
                inProgress = false;
                success = false;
                break;
            }
        }

        // Still in progress
        if (inProgress) {
            log.info("Generation '{}' is still in progress", generation.id());
            return;
        }

        if (!success) {
            Map<String, String> erroredAnnotations = erroredTaskRun.getMetadata().getAnnotations();
            GenerationResult result = isOomKilled(erroredTaskRun) ? GenerationResult.ERR_OOM
                    : GenerationResult.ERR_GENERAL;
            int retryCount = Integer.parseInt(erroredAnnotations.getOrDefault(ANNOTATION_RETRY_COUNT, "0"));
            GenerationRequest request = JacksonUtils.parse(GenerationRequest.class, generation.request());
            SyftContainerImageOptions options = retrieveOptions(request);
            int maxCount = options.retries().maxCount();

            if (result == GenerationResult.ERR_GENERAL || ++retryCount > maxCount) {
                String detailedFailureMessage = getDetailedFailureMessage(erroredTaskRun);
                updateStatus(
                        generation.id(),
                        GenerationStatus.FAILED,
                        result,
                        "Generation failed, the TaskRun returned failure: {}",
                        detailedFailureMessage);
            } else {
                log.info("Retrying Tekton Task Run for generation '{}', count '{}'", generation.id(), retryCount);
                double memoryMultiplier = options.retries().memoryMultiplier();
                TaskRun taskRun = createRetry(erroredTaskRun, retryCount, memoryMultiplier);

                try {
                    // Prevent last task run continuously being rescheduled
                    kubernetesClient.resources(TaskRun.class).resource(erroredTaskRun).delete();
                    kubernetesClient.resources(TaskRun.class).resource(taskRun).create();
                } catch (KubernetesClientException e) {
                    log.warn("Unable to schedule Tekton TaskRun retry", e);

                    updateStatus(
                            generation.id(),
                            GenerationStatus.FAILED,
                            GenerationResult.ERR_SYSTEM,
                            "Unable to schedule Tekton TaskRun retry: {}",
                            e.getMessage());
                }
            }

            return;
        }

        // Construct the path to the working directory of the generator
        Path generationDir = Path.of(controllerConfig.sbomDir(), generation.id());

        log.debug("Reading manifests from '{}'...", generationDir.toAbsolutePath());

        List<Path> manifestPaths;

        try {
            manifestPaths = FileUtils.findManifests(generationDir);
        } catch (IOException e) {
            log.error("Unexpected IO exception occurred while trying to find generated manifests", e);

            updateStatus(
                    generation.id(),
                    GenerationStatus.FAILED,
                    GenerationResult.ERR_SYSTEM,
                    "Generation succeeded, but reading generated SBOMs failed due IO exception. See logs for more information.");

            return;
        }

        if (manifestPaths.isEmpty()) {
            log.error("No manifests found, this is unexpected");

            updateStatus(
                    generation.id(),
                    GenerationStatus.FAILED,
                    GenerationResult.ERR_SYSTEM,
                    "Generation succeed, but no manifests could be found. At least one was expected. See logs for more information.");
            return;
        }

        // Read manifests
        List<JsonNode> boms;

        try {
            boms = JacksonUtils.readBoms(manifestPaths);
        } catch (Exception e) {
            log.error("Unable to read one or more manifests", e);

            updateStatus(
                    generation.id(),
                    GenerationStatus.FAILED,
                    GenerationResult.ERR_SYSTEM,
                    "Generation succeeded, but reading generated manifests failed was not successful. See logs for more information.");

            return;
        }

        // TODO: Validate manifests

        // Store manifests
        List<ManifestRecord> manifests;

        try {
            manifests = storeBoms(generation, boms);
        } catch (ValidationException e) {
            // There was an error when validating the entity, most probably the SBOM is not valid
            log.error("Unable to validate generated SBOMs: {}", e.getMessage(), e);

            updateStatus(
                    generation.id(),
                    GenerationStatus.FAILED,
                    GenerationResult.ERR_SYSTEM,
                    "Generation failed. One or more generated SBOMs failed validation: {}. See logs for more information.",
                    e.getMessage());

            return;
        }

        try {
            // syftImageController.performPost(sboms); // TODO: add this back
        } catch (ApplicationException e) {
            updateStatus(generation.id(), GenerationStatus.FAILED, GenerationResult.ERR_POST, e.getMessage());
            return;
        }

        updateStatus(
                generation.id(),
                GenerationStatus.FINISHED,
                GenerationResult.SUCCESS,
                "Generation finished successfully");
    }

    private TaskRunStepOverride resourceOverrides(GenerationRequest request) {

        return new TaskRunStepOverrideBuilder().withName(GENERATE_OVERRIDE)
                .withNewResources()
                .withRequests(
                        Map.of(
                                CPU_OVERRIDE,
                                new Quantity(request.generator().config().resources().requests().cpu()),
                                MEMORY_OVERRIDE,
                                new Quantity(request.generator().config().resources().requests().memory())))
                .withLimits(
                        Map.of(
                                CPU_OVERRIDE,
                                new Quantity(request.generator().config().resources().limits().cpu()),
                                MEMORY_OVERRIDE,
                                new Quantity(request.generator().config().resources().limits().memory())))
                .endResources()
                .build();

    }

    private TaskRunStepOverride multiplyMemoryOverrides(TaskRunStepOverride originalStepOverride, double multiplier) {
        ResourceRequirements resources = originalStepOverride.getResources();
        Quantity cpuRequestsQuantity = resources.getRequests().get(CPU_OVERRIDE);
        Quantity memoryRequestsQuantity = resources.getRequests().get(MEMORY_OVERRIDE);
        Quantity cpuLimitsQuantity = resources.getLimits().get(CPU_OVERRIDE);
        Quantity memoryLimitsQuantity = resources.getLimits().get(MEMORY_OVERRIDE);

        return new TaskRunStepOverrideBuilder().withName(GENERATE_OVERRIDE)
                .withNewResources()
                .withRequests(
                        Map.of(
                                CPU_OVERRIDE,
                                new Quantity(cpuRequestsQuantity.getAmount(), cpuRequestsQuantity.getFormat()),
                                MEMORY_OVERRIDE,
                                multiplyMemory(memoryRequestsQuantity, multiplier)))
                .withLimits(
                        Map.of(
                                CPU_OVERRIDE,
                                new Quantity(cpuLimitsQuantity.getAmount(), cpuLimitsQuantity.getFormat()),
                                MEMORY_OVERRIDE,
                                multiplyMemory(memoryLimitsQuantity, multiplier)))
                .endResources()
                .build();
    }

    private Quantity multiplyMemory(Quantity originalQuantity, double multiplier) {
        int value = Integer.parseInt(originalQuantity.getAmount());
        return new Quantity((int) Math.ceil(value * multiplier) + originalQuantity.getFormat());
    }

    private void configureOwner(TaskRun taskRun) {
        Deployment deployment = kubernetesClient.apps().deployments().withName(release + SERVICE_SUFFIX).get();

        if (deployment != null) {
            log.debug("Setting SBOMer deployment as the owner for the newly created TaskRun");

            taskRun.getMetadata()
                    .setOwnerReferences(
                            Collections.singletonList(
                                    new OwnerReferenceBuilder().withKind(HasMetadata.getKind(Deployment.class))
                                            .withApiVersion(HasMetadata.getApiVersion(Deployment.class))
                                            .withName(release + SERVICE_SUFFIX)
                                            .withUid(deployment.getMetadata().getUid())
                                            .build()));
        }
    }

    private TaskRun createRetry(TaskRun originalTaskRun, int retryCount, double memoryMultiplier) {
        ObjectMeta originalMetadata = originalTaskRun.getMetadata();
        TaskRunSpec originalSpec = originalTaskRun.getSpec();
        TaskRunStepOverride originalStepOverride = originalTaskRun.getSpec().getStepOverrides().get(0);
        String originalName = originalMetadata.getName();

        TaskRunStepOverride stepOverride = multiplyMemoryOverrides(originalStepOverride, memoryMultiplier);

        String name = originalName.replaceFirst(RETRY_SUFFIX + "\\d+$", "") + RETRY_SUFFIX + retryCount;

        TaskRun taskRun = new TaskRunBuilder().withNewMetadata()
                .withLabels(originalMetadata.getLabels())
                .withName(name)
                .addToAnnotations(ANNOTATION_RETRY_COUNT, String.valueOf(retryCount))
                .endMetadata()
                .withNewSpec()
                .withServiceAccountName(originalSpec.getServiceAccountName())
                .withTimeout(originalSpec.getTimeout())
                .withParams(originalSpec.getParams())
                .withTaskRef(originalSpec.getTaskRef())
                .withStepOverrides(stepOverride)
                .withWorkspaces(originalSpec.getWorkspaces())
                .endSpec()
                .build();

        configureOwner(taskRun);

        return taskRun;
    }

    @Override
    public TaskRun desired(GenerationRecord generation) {

        MDCUtils.removeOtelContext();
        // MDCUtils.addOtelContext(generationRequest.getMDCOtel()); TODO: add this

        log.debug(
                "Preparing dependent resource for the '{}' phase related to Generation with id '{}'",
                SbomGenerationPhase.GENERATE,
                generation.id());

        // TODO: populate traces when we create generations
        // TODO: make this a utility maybe
        Map<String, String> labels = new HashMap<>();

        labels.put(AbstractTektonController.GENERATION_ID_LABEL, generation.id());
        labels.put(AbstractTektonController.GENERATOR_TYPE, getGeneratorName());

        Optional.ofNullable(generation.metadata())
                .map(meta -> meta.get("otelTraceId"))
                .filter(JsonNode::isTextual)
                .map(JsonNode::asText)
                .ifPresent(traceId -> labels.put(Labels.LABEL_OTEL_TRACE_ID, traceId));

        Optional.ofNullable(generation.metadata())
                .map(meta -> meta.get("otelSpanId"))
                .filter(JsonNode::isTextual)
                .map(JsonNode::asText)
                .ifPresent(traceId -> labels.put(Labels.LABEL_OTEL_SPAN_ID, traceId));

        Optional.ofNullable(generation.metadata())
                .map(meta -> meta.get("otelTraceParent"))
                .filter(JsonNode::isTextual)
                .map(JsonNode::asText)
                .ifPresent(traceId -> labels.put(Labels.LABEL_OTEL_TRACEPARENT, traceId));

        GenerationRequest request = JacksonUtils.parse(GenerationRequest.class, generation.request());

        SyftContainerImageOptions options = retrieveOptions(request);

        // SyftOptions options = (SyftOptions) request.generator().config().options();

        Duration timeout;

        String timeoutSetting = Optional.ofNullable(options.timeout()).orElse("6h");

        // Parse duration
        try {
            timeout = Duration.parse(timeoutSetting);
        } catch (ParseException e) {
            throw new ApplicationException(
                    "Cannot set timeout, provided value: '{}' is invalid duration",
                    timeoutSetting,
                    e);
        }

        String taskSuffix = null;
        List<Param> params = new ArrayList<>();

        // Select Tekton Task and set parameters depending on the type of the target
        switch (request.target().type()) {
            case "CONTAINER_IMAGE":
                taskSuffix = TASK_SUFFIX;

                params.add(
                        new ParamBuilder().withName(PARAM_COMMAND_CONTAINER_IMAGE)
                                .withNewValue(request.target().identifier())
                                .build());
                params.add(
                        new ParamBuilder().withName(PARAM_PATHS)
                                .withValue(
                                        new ParamValue(
                                                Objects.requireNonNullElse(options.paths(), Collections.emptyList())))
                                .build());
                /*
                 * TODO: We use a hardcoded value here. This will be externalized to a custom processor listening on
                 * generation finished event.
                 */
                params.add(new ParamBuilder().withName(PARAM_PROCESSORS).withNewValue("default").build());
                params.add(
                        new ParamBuilder().withName(PARAM_RPMS)
                                .withValue(new ParamValue(Boolean.toString(options.includeRpms())))
                                .build());
                break;

            default:
                break;
        }

        if (taskSuffix == null) {
            throw new ApplicationException("Unknown target type: {}", request.target().type());
        }

        TaskRun taskRun = new TaskRunBuilder().withNewMetadata()
                .withLabels(labels)
                // TODO: this should be a method
                .withName(
                        "generation-" + generation.id().toLowerCase() + "-" + SbomGenerationPhase.GENERATE.ordinal()
                                + "-" + SbomGenerationPhase.GENERATE.name().toLowerCase())
                .addToAnnotations(ANNOTATION_RETRY_COUNT, "0")
                .endMetadata()
                .withNewSpec()
                .withServiceAccountName(release + SA_SUFFIX)
                .withTimeout(timeout)
                .withParams(params)
                .withTaskRef(new TaskRefBuilder().withName(release + taskSuffix).build())
                .withStepOverrides(resourceOverrides(request))
                .withWorkspaces(
                        new WorkspaceBindingBuilder().withSubPath(generation.id())
                                .withName("data")
                                .withPersistentVolumeClaim(
                                        new PersistentVolumeClaimVolumeSourceBuilder().withClaimName(release + "-sboms")
                                                .build())
                                .build())
                .endSpec()
                .build();

        configureOwner(taskRun);

        return taskRun;
    }

    private SyftContainerImageOptions retrieveOptions(GenerationRequest request) {
        JsonNode jsonNode = request.generator().config().options();
        try {
            return ObjectMapperProvider.json().treeToValue(jsonNode, SyftContainerImageOptions.class);
        } catch (JsonProcessingException e) {
            throw new ApplicationException(
                    "Unexpected options provided, expected Syft generator options, but got: {}",
                    jsonNode,
                    e);
        }
    }
}
