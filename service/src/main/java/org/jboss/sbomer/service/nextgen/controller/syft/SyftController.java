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
package org.jboss.sbomer.service.nextgen.controller.syft;

import java.io.IOException;
import java.nio.file.Path;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.cyclonedx.model.Bom;
import org.eclipse.microprofile.context.ManagedExecutor;
import org.jboss.sbomer.core.errors.ApplicationException;
import org.jboss.sbomer.core.features.sbom.enums.GenerationRequestType;
import org.jboss.sbomer.core.features.sbom.utils.FileUtils;
import org.jboss.sbomer.core.features.sbom.utils.MDCUtils;
import org.jboss.sbomer.core.features.sbom.utils.ObjectMapperProvider;
import org.jboss.sbomer.core.features.sbom.utils.SbomUtils;
import org.jboss.sbomer.service.feature.sbom.config.GenerationRequestControllerConfig;
import org.jboss.sbomer.service.feature.sbom.k8s.model.GenerationRequest;
import org.jboss.sbomer.service.feature.sbom.k8s.model.SbomGenerationPhase;
import org.jboss.sbomer.service.feature.sbom.k8s.resources.Labels;
import org.jboss.sbomer.service.feature.sbom.model.Sbom;
import org.jboss.sbomer.service.nextgen.controller.AbstractController;
import org.jboss.sbomer.service.nextgen.controller.TaskRunEventProvider;
import org.jboss.sbomer.service.nextgen.controller.request.Request;
import org.jboss.sbomer.service.nextgen.core.dto.GenerationRecord;
import org.jboss.sbomer.service.nextgen.core.dto.EntityMapper;
import org.jboss.sbomer.service.nextgen.core.enums.GenerationResult;
import org.jboss.sbomer.service.nextgen.core.enums.GenerationStatus;
import org.jboss.sbomer.service.nextgen.core.events.GenerationScheduledEvent;
import org.jboss.sbomer.service.nextgen.core.events.GenerationStateChangedEvent;
import org.jboss.sbomer.service.nextgen.service.model.Generation;
import org.jboss.sbomer.service.nextgen.service.model.Manifest;

import com.fasterxml.jackson.core.JsonProcessingException;

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
import io.quarkus.arc.Arc;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.TransactionPhase;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.ValidationException;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@ApplicationScoped
@NoArgsConstructor
@Slf4j
public class SyftController extends AbstractController {
    public static final String PARAM_COMMAND_CONTAINER_IMAGE = "image";
    public static final String PARAM_COMMAND_TYPE = "type";
    public static final String PARAM_COMMAND_IDENTIFIER = "identifier";
    public static final String PARAM_PATHS = "paths";
    public static final String PARAM_RPMS = "rpms";
    public static final String PARAM_PROCESSORS = "processors";
    public static final String SA_SUFFIX = "-sa";
    public static final String TASK_SUFFIX = "-generator-syft";

    public static final String GENERATOR_NAME = "syft";

    @Inject
    public SyftController(
            KubernetesClient kubernetesClient,
            GenerationRequestControllerConfig controllerConfig,
            ManagedExecutor managedExecutor,
            EntityMapper mapper) {
        super(kubernetesClient, controllerConfig, managedExecutor, mapper);
    }

    public void onEvent(@Observes(during = TransactionPhase.AFTER_SUCCESS) GenerationScheduledEvent event) {
        if (!event.isOfRequestType("CONTAINER_IMAGE")) {
            // This is not an event handled by this listener
            return;
        }

        managedExecutor.runAsync(() -> {
            reconcile(event.generation(), Collections.emptySet());
        });
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
                    generation,
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
                    generation,
                    GenerationStatus.FAILED,
                    GenerationResult.ERR_SYSTEM,
                    "Generation succeeded, but reading generated SBOMs failed due IO exception. See logs for more information.");

            return;
        }

        if (manifestPaths.isEmpty()) {
            log.error("No manifests found, this is unexpected");

            updateStatus(
                    generation,
                    GenerationStatus.FAILED,
                    GenerationResult.ERR_SYSTEM,
                    "Generation succeed, but no manifests could be found. At least one was expected. See logs for more information.");
            return;
        }

        List<Bom> boms;

        try {
            boms = readManifests(manifestPaths);
        } catch (Exception e) {
            log.error("Unable to read one or more manifests", e);

            updateStatus(
                    generation,
                    GenerationStatus.FAILED,
                    GenerationResult.ERR_SYSTEM,
                    "Generation succeeded, but reading generated manifests failed was not successful. See logs for more information.");

            return;
        }

        List<Manifest> sboms;

        try {
            sboms = storeBoms(generation, boms);
        } catch (ValidationException e) {
            // There was an error when validating the entity, most probably the SBOM is not valid
            log.error("Unable to validate generated SBOMs: {}", e.getMessage(), e);

            updateStatus(
                    generation,
                    GenerationStatus.FAILED,
                    GenerationResult.ERR_SYSTEM,
                    "Generation failed. One or more generated SBOMs failed validation: {}. See logs for more information.",
                    e.getMessage());

            return;
        }

        try {
            // syftImageController.performPost(sboms); // TODO: add thios back
        } catch (ApplicationException e) {
            updateStatus(generation, GenerationStatus.FAILED, GenerationResult.ERR_POST, e.getMessage());
            return;
        }

        updateStatus(
                generation,
                GenerationStatus.FINISHED,
                GenerationResult.SUCCESS,
                "Generation finished successfully");

        Arc.container().beanManager().getEvent().fire(new GenerationStateChangedEvent(generation));
    }

    private TaskRunStepOverride resourceOverrides(Request request) {

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
    protected TaskRun desired(GenerationRecord generation) {

        MDCUtils.removeOtelContext();
        // MDCUtils.addOtelContext(generationRequest.getMDCOtel()); TODO: add this

        log.debug(
                "Preparing dependent resource for the '{}' phase related to Generation with id '{}'",
                SbomGenerationPhase.GENERATE,
                generation.id());
        Map<String, String> labels = Labels.defaultLabelsToMap(GenerationRequestType.CONTAINERIMAGE);

        labels.put(Labels.LABEL_PHASE, SbomGenerationPhase.GENERATE.name().toLowerCase());
        labels.put(TaskRunEventProvider.GENERATION_ID_LABEL, generation.id());
        // labels.put(Labels.LABEL_OTEL_TRACE_ID, generationRequest.getTraceId()); TODO: add this
        // labels.put(Labels.LABEL_OTEL_SPAN_ID, generationRequest.getSpanId());
        // labels.put(Labels.LABEL_OTEL_TRACEPARENT, generationRequest.getTraceParent());

        Request request = Request.parse(generation);

        SyftOptions options = null;

        try {
            options = ObjectMapperProvider.json()
                    .treeToValue(request.generator().config().options(), SyftOptions.class);
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

    /**
     * <p>
     * Stores the generated manifests in the database which results in creation of new {@link Sbom}s entities.
     * </p>
     *
     * <p>
     * Additionally updates the database entity with the current state of the {@link GenerationRequest}.
     * </p>
     *
     * @param generation the generation request
     * @param boms the BOMs to store
     * @return the list of stored {@link Sbom}s
     */
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public List<Manifest> storeBoms(GenerationRecord generationRecord, List<Bom> boms) {
        // TODO @avibelli
        MDCUtils.removeOtelContext();
        MDCUtils.addIdentifierContext(generationRecord.id());
        // MDCUtils.addOtelContext(generation.getMDCOtel());

        // TODO @avibelli
        // Maybe we should add it later, when we will be transitioning this into a release manifest?
        // Syft controller should be generic and not know anything about RH internals.

        // Verify if the request event for this generation is associated with an Errata advisory
        // RequestEvent event = sbomGenerationRequest.getRequest();
        // if (event != null && event.getRequestConfig() != null
        // && event.getRequestConfig() instanceof ErrataAdvisoryRequestConfig config) {

        // boms.forEach(bom -> {
        // // Add the AdvisoryId property
        // addPropertyIfMissing(
        // bom.getMetadata(),
        // Constants.CONTAINER_PROPERTY_ADVISORY_ID,
        // config.getAdvisoryId());
        // });
        // }

        log.info("There are {} manifests to be stored for the {} generation...", boms.size(), generationRecord.id());

        // Find the Generation
        Generation generation = Generation.findById(generationRecord.id());

        if (generation == null) {
            throw new ApplicationException("Unable to find Generation with ID '{}'", generationRecord.id());
        }

        List<Manifest> manifests = new ArrayList<>();

        // Create Manifest entities for all manifests
        boms.forEach(bom -> {
            log.info("Storing manifests for the Generation '{}'", generationRecord.id());
            manifests.add(
                    Manifest.builder().withSbom(SbomUtils.toJsonNode(bom)).withGeneration(generation).build().save());
        });

        return manifests;
    }

}
