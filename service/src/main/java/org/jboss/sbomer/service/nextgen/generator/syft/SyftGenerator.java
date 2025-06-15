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

import org.eclipse.microprofile.context.ManagedExecutor;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.sbomer.core.errors.ApplicationException;
import org.jboss.sbomer.core.features.sbom.utils.FileUtils;
import org.jboss.sbomer.core.features.sbom.utils.MDCUtils;
import org.jboss.sbomer.core.features.sbom.utils.ObjectMapperProvider;
import org.jboss.sbomer.service.feature.sbom.config.GenerationRequestControllerConfig;
import org.jboss.sbomer.service.feature.sbom.k8s.model.SbomGenerationPhase;
import org.jboss.sbomer.service.feature.sbom.k8s.resources.Labels;
import org.jboss.sbomer.service.nextgen.controller.tekton.AbstractTektonController;
import org.jboss.sbomer.service.nextgen.controller.tekton.GenerationTaskRunEventProvider;
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
import io.fabric8.kubernetes.api.model.OwnerReferenceBuilder;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaimVolumeSourceBuilder;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.tekton.v1beta1.Param;
import io.fabric8.tekton.v1beta1.ParamBuilder;
import io.fabric8.tekton.v1beta1.ParamValue;
import io.fabric8.tekton.v1beta1.TaskRefBuilder;
import io.fabric8.tekton.v1beta1.TaskRun;
import io.fabric8.tekton.v1beta1.TaskRunBuilder;
import io.fabric8.tekton.v1beta1.TaskRunStepOverride;
import io.fabric8.tekton.v1beta1.TaskRunStepOverrideBuilder;
import io.fabric8.tekton.v1beta1.WorkspaceBindingBuilder;
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

    public static final String GENERATOR_NAME = "syft";

    private SyftGenerator() {
        super(null, null, null, null, null);
    }

    @Inject
    public SyftGenerator(
            @RestClient SBOMerClient sbomerClient,
            KubernetesClient kubernetesClient,
            GenerationRequestControllerConfig controllerConfig,
            ManagedExecutor managedExecutor,
            EntityMapper mapper) {
        super(sbomerClient, kubernetesClient, controllerConfig, managedExecutor, mapper);
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
        return "1.16.0";
    }

    @Override
    public void generate(GenerationRecord generationRecord) {
        reconcile(generationRecord, Collections.emptySet());
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
            String detailedFailureMessage = getDetailedFailureMessage(erroredTaskRun);
            updateStatus(
                    generation.id(),
                    GenerationStatus.FAILED,
                    GenerationResult.ERR_GENERAL,
                    "Generation failed, the TaskRun returned failure: {}",
                    detailedFailureMessage);

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

        return new TaskRunStepOverrideBuilder().withName("generate")
                .withNewResources()
                .withRequests(
                        Map.of(
                                "cpu",
                                new Quantity(request.generator().config().resources().requests().cpu()),
                                "memory",
                                new Quantity(request.generator().config().resources().requests().memory())))
                .withLimits(
                        Map.of(
                                "cpu",
                                new Quantity(request.generator().config().resources().limits().cpu()),
                                "memory",
                                new Quantity(request.generator().config().resources().limits().memory())))
                .endResources()
                .build();

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

        labels.put(GenerationTaskRunEventProvider.GENERATION_ID_LABEL, generation.id());

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

        SyftContainerImageOptions options = null;

        try {
            options = ObjectMapperProvider.json()
                    .treeToValue(request.generator().config().options(), SyftContainerImageOptions.class);
        } catch (JsonProcessingException e) {
            throw new ApplicationException(
                    "Unexpected options provided, expected Syft generator options, but got: {}",
                    request.generator().config().options(),
                    e);
        }

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

        Deployment deployment = kubernetesClient.apps().deployments().withName(release + "-service").get();

        if (deployment != null) {
            log.debug("Setting SBOMer deployment as the owner for the newly created TaskRun");

            taskRun.getMetadata()
                    .setOwnerReferences(
                            Collections.singletonList(
                                    new OwnerReferenceBuilder().withKind(HasMetadata.getKind(Deployment.class))
                                            .withApiVersion(HasMetadata.getApiVersion(Deployment.class))
                                            .withName(release + "-service")
                                            .withUid(deployment.getMetadata().getUid())
                                            .build()));
        }
        return taskRun;
    }
}
