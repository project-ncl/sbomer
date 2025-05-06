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
package org.jboss.sbomer.service.feature.sbom.k8s.reconciler;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.cyclonedx.model.Bom;
import org.jboss.sbomer.core.errors.ApplicationException;
import org.jboss.sbomer.core.features.sbom.config.Config;
import org.jboss.sbomer.core.features.sbom.config.PncBuildConfig;
import org.jboss.sbomer.core.features.sbom.enums.GenerationRequestType;
import org.jboss.sbomer.core.features.sbom.enums.GenerationResult;
import org.jboss.sbomer.core.features.sbom.utils.MDCUtils;
import org.jboss.sbomer.core.features.sbom.utils.ObjectMapperProvider;
import org.jboss.sbomer.core.features.sbom.utils.SbomUtils;
import org.jboss.sbomer.service.feature.sbom.features.generator.AbstractController;
import org.jboss.sbomer.service.feature.sbom.k8s.model.GenerationRequest;
import org.jboss.sbomer.service.feature.sbom.k8s.model.SbomGenerationPhase;
import org.jboss.sbomer.service.feature.sbom.k8s.model.SbomGenerationStatus;
import org.jboss.sbomer.service.feature.sbom.k8s.reconciler.condition.IsBuildTypeCondition;
import org.jboss.sbomer.service.feature.sbom.k8s.reconciler.condition.IsBuildTypeInitializedCondition;
import org.jboss.sbomer.service.feature.sbom.k8s.resources.Labels;
import org.jboss.sbomer.service.feature.sbom.k8s.resources.TaskRunGenerateBuildDependentResource;
import org.jboss.sbomer.service.feature.sbom.k8s.resources.TaskRunInitDependentResource;
import org.jboss.sbomer.service.feature.sbom.model.RandomStringIdGenerator;
import org.jboss.sbomer.service.feature.sbom.model.Sbom;
import org.jboss.sbomer.service.feature.sbom.model.SbomGenerationRequest;
import org.slf4j.helpers.MessageFormatter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.fabric8.tekton.v1beta1.Param;
import io.fabric8.tekton.v1beta1.TaskRun;
import io.fabric8.tekton.v1beta1.TaskRunResult;
import io.javaoperatorsdk.operator.api.config.informer.Informer;
import io.javaoperatorsdk.operator.api.reconciler.Constants;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ControllerConfiguration;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;
import io.javaoperatorsdk.operator.api.reconciler.Workflow;
import io.javaoperatorsdk.operator.api.reconciler.dependent.Dependent;
import jakarta.transaction.Transactional;
import jakarta.validation.ValidationException;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>
 * Reconciler working on the {@link GenerationRequest} entity and the {@link GenerationRequestType#BUILD} type.
 * </p>
 *
 * <p>
 * This reconciler acts only on resources marked with the following labels (all of them must exist on the resource):
 *
 * <ul>
 * <li>{@code app.kubernetes.io/part-of=sbomer}</li>
 * <li>{@code app.kubernetes.io/component=sbom}</li>
 * <li>{@code app.kubernetes.io/managed-by=sbom}</li>
 * <li>{@code sbomer.jboss.org/generation-request}</li>
 * <li>{@code sbomer.jboss.org/generation-request-type}</li>
 * </ul>
 * </p>
 */
@ControllerConfiguration(
        informer = @Informer(
                namespaces = { Constants.WATCH_CURRENT_NAMESPACE },
                labelSelector = "app.kubernetes.io/part-of=sbomer,app.kubernetes.io/managed-by=sbomer,app.kubernetes.io/component=generator,sbomer.jboss.org/type=generation-request,sbomer.jboss.org/generation-request-type=build"))
@Workflow(
        dependents = {
                @Dependent(
                        useEventSourceWithName = "tekton-generation-request-build",
                        type = TaskRunInitDependentResource.class,
                        reconcilePrecondition = IsBuildTypeCondition.class),
                @Dependent(
                        useEventSourceWithName = "tekton-generation-request-build",
                        type = TaskRunGenerateBuildDependentResource.class,
                        reconcilePrecondition = IsBuildTypeInitializedCondition.class) })
@Slf4j
public class BuildController extends AbstractController {
    final ObjectMapper objectMapper = ObjectMapperProvider.yaml();

    @Override
    protected GenerationRequestType generationRequestType() {
        return GenerationRequestType.BUILD;
    }

    @Override
    protected UpdateControl<GenerationRequest> updateRequest(
            GenerationRequest generationRequest,
            SbomGenerationStatus status,
            GenerationResult result,
            String reason,
            Object... params) {

        if (generationRequest.getStatus() != null) {
            String label = switch (generationRequest.getStatus()) {
                case INITIALIZING -> SbomGenerationPhase.INIT.name().toLowerCase();
                case GENERATING -> SbomGenerationPhase.GENERATE.name().toLowerCase();
                default -> null;
            };

            if (label != null) {
                generationRequest.getMetadata().getLabels().put(Labels.LABEL_PHASE, label);
            }
        }

        generationRequest.setStatus(status);
        generationRequest.setResult(result);
        generationRequest.setReason(MessageFormatter.arrayFormat(reason, params).getMessage());

        return UpdateControl.patchResource(generationRequest);
    }

    /**
     * Possible next statuses: {@link SbomGenerationStatus#FAILED}, {@link SbomGenerationStatus#INITIALIZED}
     *
     * @param secondaryResources the secondary resources
     * @param generationRequest the generation request
     *
     * @return the update control for this generation request
     */
    private UpdateControl<GenerationRequest> reconcileInitializing(
            GenerationRequest generationRequest,
            Set<TaskRun> secondaryResources) {

        log.debug("ReconcileInitializing ...");

        TaskRun initTaskRun = findTaskRun(secondaryResources, SbomGenerationPhase.INIT);

        if (initTaskRun == null) {
            log.error(
                    "There is no initialization TaskRun related to GenerationRequest '{}'",
                    generationRequest.getName());

            return updateRequest(
                    generationRequest,
                    SbomGenerationStatus.FAILED,
                    GenerationResult.ERR_SYSTEM,
                    "Configuration initialization failed. Unable to find related TaskRun. See logs for more information.");
        }

        if (!isFinished(initTaskRun)) {
            return UpdateControl.noUpdate();
        }

        if (Boolean.TRUE.equals(isSuccessful(initTaskRun))) {
            setConfig(generationRequest, initTaskRun);
            return updateRequest(generationRequest, SbomGenerationStatus.INITIALIZED, null, null);
        }

        StringBuilder sb = new StringBuilder("Configuration initialization failed. ");

        GenerationResult result;

        if (initTaskRun.getStatus() != null && initTaskRun.getStatus().getSteps() != null
                && !initTaskRun.getStatus().getSteps().isEmpty()
                && initTaskRun.getStatus().getSteps().get(0).getTerminated() != null) {

            Optional<GenerationResult> optResult = GenerationResult
                    .fromCode(initTaskRun.getStatus().getSteps().get(0).getTerminated().getExitCode());

            if (optResult.isPresent()) {
                result = optResult.get();

                // At this point the config generation failed, let's try to provide more info on the failure
                switch (result) {
                    case ERR_GENERAL:
                        sb.append("General error occurred. ");
                        break;
                    case ERR_CONFIG_INVALID:
                        sb.append("Configuration validation failed. ");
                        break;
                    case ERR_CONFIG_MISSING:
                        sb.append("Could not find configuration. ");
                        break;
                    case ERR_SYSTEM:
                        sb.append("System error occurred. ");
                        break;
                    default:
                        // In case we don't have a mapped exit code, we assume it is a system error
                        result = GenerationResult.ERR_SYSTEM;
                        sb.append("Unexpected error occurred. ");
                        break;
                }
            } else {
                log.warn(
                        "Unknown exit code received from the finished '{}' TaskRun: {}",
                        initTaskRun.getMetadata().getName(),
                        initTaskRun.getStatus().getSteps().get(0).getTerminated().getExitCode());
                result = GenerationResult.ERR_SYSTEM;
                sb.append("Unknown exit code received. ");
            }
        } else {
            result = GenerationResult.ERR_SYSTEM;
            sb.append("System failure. ");
        }

        String reason = sb.append("See logs for more information.").toString();

        log.warn("GenerationRequest '{}' failed. {}", generationRequest.getName(), reason);

        return updateRequest(generationRequest, SbomGenerationStatus.FAILED, result, reason);
    }

    /**
     * Possible next statuses: {@link SbomGenerationStatus#GENERATING}
     *
     * @param secondaryResources the secondary resources
     * @param generationRequest the generation request
     *
     * @return the update control for this generation request
     */
    private UpdateControl<GenerationRequest> reconcileInitialized(
            GenerationRequest generationRequest,
            Set<TaskRun> secondaryResources) {

        log.debug("ReconcileInitialized ...");
        PncBuildConfig config = generationRequest.getConfig(PncBuildConfig.class);

        if (config == null) {

            log.error(
                    "Product configuration from GenerationRequest '{}' could not be read",
                    generationRequest.getName());
            return updateRequest(
                    generationRequest,
                    SbomGenerationStatus.FAILED,
                    GenerationResult.ERR_SYSTEM,
                    "Generation failed. Could not read product configuration");
        }

        Set<TaskRun> generateTaskRuns = findTaskRuns(secondaryResources, SbomGenerationPhase.GENERATE);
        if (generateTaskRuns.isEmpty()) {
            return UpdateControl.noUpdate();
        }

        return updateRequest(generationRequest, SbomGenerationStatus.GENERATING, null, null);
    }

    // TODO: Refactor
    @Override
    protected UpdateControl<GenerationRequest> reconcileGenerating(
            GenerationRequest generationRequest,
            Set<TaskRun> secondaryResources) {

        log.debug("ReconcileGenerating ...");

        PncBuildConfig config = generationRequest.getConfig(PncBuildConfig.class);

        if (config == null) {
            log.error(
                    "Product configuration from GenerationRequest '{}' could not be read",
                    generationRequest.getName());

            return updateRequest(
                    generationRequest,
                    SbomGenerationStatus.FAILED,
                    GenerationResult.ERR_SYSTEM,
                    "Generation failed. Could not read product configuration");
        }

        Set<TaskRun> generateTaskRuns = findTaskRuns(secondaryResources, SbomGenerationPhase.GENERATE);

        // This should not happen, because if we already updated the status to 'SbomGenerationStatus.GENERATING', then
        // there was a TaskRun already.
        // But it could be deleted manually(?), so in such a case, we set the status to 'FAILED'.
        if (generateTaskRuns.isEmpty()) {
            log.error(
                    "Marking GenerationRequest '{}' as failed: no generation TaskRuns were found, but at least one was expected.",
                    generationRequest.getName());

            return updateRequest(
                    generationRequest,
                    SbomGenerationStatus.FAILED,
                    GenerationResult.ERR_SYSTEM,
                    "Generation failed. Expected one or more running TaskRun related to generation. None found. See logs for more information.");
        }

        log.debug(
                "Found {} TaskRuns related to GenerationRequest '{}'",
                generateTaskRuns.size(),
                generationRequest.getName());

        // Check for still running tasks
        List<TaskRun> stillRunning = generateTaskRuns.stream().filter(tr -> !isFinished(tr)).toList();

        // If there are tasks that haven't finished yet, we need to wait.
        if (!stillRunning.isEmpty()) {
            log.debug(
                    "Skipping update of GenerationRequest '{}', because {} TaskRuns are still running: {}",
                    generationRequest.getName(),
                    stillRunning.size(),
                    stillRunning.stream().map(tr -> tr.getMetadata().getName()).toArray());
            return UpdateControl.noUpdate();
        }

        // Get list of failed TaskRuns
        List<TaskRun> failedTaskRuns = generateTaskRuns.stream()
                .filter(tr -> !isSuccessful(tr))
                .sorted(Comparator.comparing(tr -> tr.getMetadata().getName()))
                .toList();

        List<Sbom> sboms = null;

        // If all tasks finished successfully
        if (failedTaskRuns.isEmpty()) {
            try {
                sboms = storeSboms(generationRequest);
            } catch (ValidationException e) {
                // There was an error when validating the entity, most probably the SBOM is not valid
                log.error("Unable to validate generated SBOM", e);

                return updateRequest(
                        generationRequest,
                        SbomGenerationStatus.FAILED,
                        GenerationResult.ERR_GENERATION,
                        "Generation failed. One or more generated SBOMs failed validation. See logs for more information.");
            }

            try {
                performPost(sboms);
            } catch (ApplicationException e) {
                return updateRequest(
                        generationRequest,
                        SbomGenerationStatus.FAILED,
                        GenerationResult.ERR_POST,
                        e.getMessage());
            }

            return updateRequest(
                    generationRequest,
                    SbomGenerationStatus.FINISHED,
                    GenerationResult.SUCCESS,
                    String.format(
                            "Generation finished successfully. Generated SBOMs: %s",
                            sboms.stream().map(Sbom::getId).collect(Collectors.joining(", "))));
        }

        StringBuilder sb = new StringBuilder("Generation request failed. ");
        GenerationResult result = GenerationResult.ERR_SYSTEM;

        // If the number of failed generations is the same as the number of products, it means that all generations
        // failed
        if (failedTaskRuns.size() == generateTaskRuns.size()) {
            sb.append("All tasks failed. ");
        } else {
            sb.append("Some tasks failed. ");
        }

        for (TaskRun taskRun : failedTaskRuns) {
            Param productIndexParam = getParamValue(taskRun, "index");

            sb.append("Product with index '")
                    .append(productIndexParam.getValue().getStringVal())
                    .append("' (TaskRun '")
                    .append(taskRun.getMetadata().getName())
                    .append("') failed: ");

            if (taskRun.getStatus() != null && taskRun.getStatus().getSteps() != null
                    && !taskRun.getStatus().getSteps().isEmpty()
                    && taskRun.getStatus().getSteps().get(0).getTerminated() != null) {

                Optional<GenerationResult> optResult = GenerationResult
                        .fromCode(taskRun.getStatus().getSteps().get(0).getTerminated().getExitCode());

                if (optResult.isPresent()) {
                    result = optResult.get();
                }

                switch (result) {
                    case ERR_GENERAL:
                        sb.append("general error occurred. ");
                        break;
                    case ERR_CONFIG_INVALID:
                        sb.append("product configuration failure. ");
                        break;
                    case ERR_INDEX_INVALID:
                        Param param = getParamValue(taskRun, "index");

                        if (param == null) {
                            sb.append("could not find the 'index' parameter. ");
                        } else {
                            sb.append("invalid product index: ")
                                    .append(productIndexParam.getValue().getStringVal())
                                    .append(" (should be between 1 and ")
                                    .append(config.getProducts().size())
                                    .append("). ");
                        }

                        break;
                    case ERR_GENERATION:
                        sb.append("an error occurred while generating the SBOM. ");
                        break;
                    default:
                        result = GenerationResult.ERR_SYSTEM;
                        sb.append("unexpected error occurred. ");
                        break;
                }
            } else {
                sb.append("system failure. ");
            }

        }

        // When we have more than one task that has failed, we need to set the result to be a multi-failure
        if (failedTaskRuns.size() > 1) {
            result = GenerationResult.ERR_MULTI;
        }

        String reason = sb.append("See logs for more information.").toString();

        log.warn("Marking GenerationRequest '{}' as failed. Reason: {}", generationRequest.getName(), reason);

        return updateRequest(generationRequest, SbomGenerationStatus.FAILED, result, reason);
    }

    private Param getParamValue(TaskRun taskRun, String paramName) {
        Optional<Param> param = taskRun.getSpec()
                .getParams()
                .stream()
                .filter(p -> p.getName().equals(paramName))
                .findFirst();

        return param.orElse(null);
    }

    /**
     * <p>
     * Possible next statuses: {@link SbomGenerationStatus#FAILED}, {@link SbomGenerationStatus#INITIALIZING} or
     * {@link SbomGenerationStatus#INITIALIZED} if it was really fast :)
     * </p>
     *
     * <p>
     * For the {@link SbomGenerationStatus#NEW} state we don't need to do anything, just wait.
     * </p>
     *
     * @param generationRequest the generation request
     * @param secondaryResources the secondary resources
     * @return Action to take on the {@link GenerationRequest} resource.
     */
    @Override
    protected UpdateControl<GenerationRequest> reconcileScheduled(
            GenerationRequest generationRequest,
            Set<TaskRun> secondaryResources) {

        log.debug("ReconcileScheduled ...");
        TaskRun initTaskRun = findTaskRun(secondaryResources, SbomGenerationPhase.INIT);

        if (initTaskRun == null) {
            return UpdateControl.noUpdate();
        }

        return updateRequest(generationRequest, SbomGenerationStatus.INITIALIZING, null, null);
    }

    @Override
    @Transactional
    public UpdateControl<GenerationRequest> reconcile(
            GenerationRequest generationRequest,
            Context<GenerationRequest> context) throws Exception {

        MDCUtils.removeContext();
        MDCUtils.addIdentifierContext(generationRequest.getIdentifier());
        MDCUtils.addOtelContext(generationRequest.getMDCOtel());

        // No status is set, it should be "NEW", let's do it.
        // "NEW" starts everything.
        if (Objects.isNull(generationRequest.getStatus())) {
            return updateRequest(generationRequest, SbomGenerationStatus.NEW, null, null);
        }

        // Fetch any secondary resources (Tekton TaskRuns) that are related to the primary resource (GenerationRequest)
        // There may be between 0 or more TaskRuns related to the GenerationRequest:
        // 0 In case these were not created yet,
        // 1 in case the initialization or environment config tasks are running
        // 2 or more in case the generation is running
        Set<TaskRun> secondaryResources = context.getSecondaryResources(TaskRun.class);

        UpdateControl<GenerationRequest> action = null;

        log.debug(
                "Handling update for GenerationRequest '{}', current status: '{}'",
                generationRequest.getMetadata().getName(),
                generationRequest.getStatus());

        switch (generationRequest.getStatus()) {
            case NEW:
                action = reconcileNew(generationRequest, secondaryResources);
                break;
            case SCHEDULED:
                action = reconcileScheduled(generationRequest, secondaryResources);
                break;
            case INITIALIZING:
                action = reconcileInitializing(generationRequest, secondaryResources);
                break;
            case INITIALIZED:
                action = reconcileInitialized(generationRequest, secondaryResources);
                break;
            case GENERATING:
                action = reconcileGenerating(generationRequest, secondaryResources);
                break;
            case FINISHED:
                action = reconcileFinished(generationRequest);
                break;
            case FAILED:
                action = reconcileFailed(generationRequest);
                break;
            default:
                break;
        }

        // This would be unexpected.
        if (action == null) {
            if (generationRequest.getStatus().equals(SbomGenerationStatus.NO_OP)) {
                log.info(
                        "No operation status received: '{}' for GenerationRequest '{}', doing nothing!",
                        generationRequest.getStatus(),
                        generationRequest.getMetadata().getName());
            } else {
                log.error(
                        "Unknown status received: '{}' for GenerationRequest '{}'",
                        generationRequest.getStatus(),
                        generationRequest.getMetadata().getName());
            }
            return UpdateControl.noUpdate();
        }

        // In case resource gets an update, update th DB entity as well
        if (action.isPatchResource()) {
            SbomGenerationRequest.sync(generationRequest);
        }

        return action;
    }

    protected List<Sbom> storeSboms(GenerationRequest generationRequest) {
        MDCUtils.removeOtelContext();
        MDCUtils.addIdentifierContext(generationRequest.getIdentifier());
        MDCUtils.addOtelContext(generationRequest.getMDCOtel());

        SbomGenerationRequest sbomGenerationRequest = SbomGenerationRequest.sync(generationRequest);

        log.info(
                "Reading all generated SBOMs for the GenerationRequest '{}'",
                generationRequest.getMetadata().getName());

        PncBuildConfig config = generationRequest.getConfig(PncBuildConfig.class);

        List<Sbom> sboms = new ArrayList<>();

        for (int i = 0; i < config.getProducts().size(); i++) {
            log.info("Reading SBOM for index '{}'", i);

            Path sbomPath = Path.of(
                    controllerConfig.sbomDir(),
                    generationRequest.getMetadata().getName(),
                    SbomGenerationPhase.GENERATE.name().toLowerCase(),
                    String.valueOf(i),
                    "bom.json");

            // Read the generated SBOM JSON file
            Bom bom = SbomUtils.fromPath(sbomPath);

            // Create the Sbom entity
            Sbom sbom = Sbom.builder()
                    .withId(RandomStringIdGenerator.generate())
                    .withIdentifier(generationRequest.getIdentifier())
                    .withSbom(SbomUtils.toJsonNode(bom))
                    .withGenerationRequest(sbomGenerationRequest)
                    .withConfigIndex(i)
                    .build();

            // And store it in the database
            sboms.add(sbomRepository.saveSbom(sbom));
        }

        return sboms;
    }

    private Config setConfig(GenerationRequest generationRequest, TaskRun taskRun) {
        log.debug("Handling result of the initialization task");

        if (taskRun.getStatus() == null) {
            throw new ApplicationException(
                    "TaskRun '{}' does not have status sub-resource despite it is expected",
                    taskRun.getMetadata().getName());

        }

        if (taskRun.getStatus().getTaskResults() == null || taskRun.getStatus().getTaskResults().isEmpty()) {
            throw new ApplicationException(
                    "TaskRun '{}' does not have any results despite it is expected to have one",
                    taskRun.getMetadata().getName());
        }

        Optional<TaskRunResult> configResult = taskRun.getStatus()
                .getTaskResults()
                .stream()
                .filter(result -> Objects.equals(result.getName(), TaskRunInitDependentResource.RESULT_NAME))
                .findFirst();

        if (configResult.isEmpty()) {
            throw new ApplicationException(
                    "Could not find the '{}' result within the TaskRun '{}'",
                    TaskRunInitDependentResource.RESULT_NAME,
                    taskRun.getMetadata().getName());
        }

        String configVal = configResult.get().getValue().getStringVal();
        // PncBuildConfig config = generationRequest.getConfig(PncBuildConfig.class);

        Config config = Config.fromString(configVal);

        // try {
        // config = objectMapper.readValue(configVal.getBytes(), Config.class);
        // } catch (IOException e) {
        // throw new ApplicationException(
        // "Could not parse the '{}' result within the TaskRun '{}': {}",
        // TaskRunInitDependentResource.RESULT_NAME,
        // taskRun.getMetadata().getName(),
        // configVal);
        // }

        log.debug("Runtime config from TaskRun '{}' parsed: {}", taskRun.getMetadata().getName(), config);

        try {
            generationRequest.setConfig(objectMapper.writeValueAsString(config));
        } catch (JsonProcessingException e) {
            log.error("Unable to serialize product configuration", e);
        }

        return config;
    }
}
