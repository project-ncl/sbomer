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
package org.jboss.sbomer.service.feature.sbom.k8s.reconciler;

import static org.jboss.sbomer.service.feature.sbom.k8s.reconciler.GenerationRequestReconciler.EVENT_SOURCE_NAME;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.cyclonedx.model.Bom;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.sbomer.core.errors.ApplicationException;
import org.jboss.sbomer.core.features.sbom.config.runtime.Config;
import org.jboss.sbomer.core.features.sbom.utils.MDCUtils;
import org.jboss.sbomer.core.features.sbom.utils.ObjectMapperProvider;
import org.jboss.sbomer.core.features.sbom.utils.SbomUtils;
import org.jboss.sbomer.service.feature.sbom.features.umb.producer.NotificationService;
import org.jboss.sbomer.service.feature.sbom.k8s.model.GenerationRequest;
import org.jboss.sbomer.service.feature.sbom.k8s.model.SbomGenerationPhase;
import org.jboss.sbomer.service.feature.sbom.k8s.model.SbomGenerationStatus;
import org.jboss.sbomer.service.feature.sbom.k8s.reconciler.condition.InitFinishedCondition;
import org.jboss.sbomer.service.feature.sbom.k8s.reconciler.condition.NotFinalOrFailedRequestCondition;
import org.jboss.sbomer.service.feature.sbom.k8s.resources.Labels;
import org.jboss.sbomer.service.feature.sbom.k8s.resources.TaskRunGenerateDependentResource;
import org.jboss.sbomer.service.feature.sbom.k8s.resources.TaskRunInitDependentResource;
import org.jboss.sbomer.service.feature.sbom.model.RandomStringIdGenerator;
import org.jboss.sbomer.service.feature.sbom.model.Sbom;
import org.jboss.sbomer.service.feature.sbom.model.SbomGenerationRequest;
import org.jboss.sbomer.service.feature.sbom.service.SbomRepository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.tekton.client.TektonClient;
import io.fabric8.tekton.pipeline.v1beta1.Param;
import io.fabric8.tekton.pipeline.v1beta1.TaskRun;
import io.fabric8.tekton.pipeline.v1beta1.TaskRunResult;
import io.javaoperatorsdk.operator.api.config.informer.InformerConfiguration;
import io.javaoperatorsdk.operator.api.reconciler.Cleaner;
import io.javaoperatorsdk.operator.api.reconciler.Constants;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ControllerConfiguration;
import io.javaoperatorsdk.operator.api.reconciler.DeleteControl;
import io.javaoperatorsdk.operator.api.reconciler.EventSourceContext;
import io.javaoperatorsdk.operator.api.reconciler.EventSourceInitializer;
import io.javaoperatorsdk.operator.api.reconciler.Reconciler;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;
import io.javaoperatorsdk.operator.api.reconciler.dependent.Dependent;
import io.javaoperatorsdk.operator.processing.event.source.EventSource;
import io.javaoperatorsdk.operator.processing.event.source.informer.InformerEventSource;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>
 * Reconciler working on the {@link GenerationRequest} being a {@link io.fabric8.kubernetes.api.model.ConfigMap}.
 * </p>
 *
 * <p>
 * This reconciler acts only on resources marked with following labels (all of them must exist on the resource):
 *
 * <ul>
 * <li>{@code app.kubernetes.io/part-of=sbomer}</li>
 * <li>{@code app.kubernetes.io/component=sbom}</li>
 * <li>{@code app.kubernetes.io/managed-by=sbom}</li>
 * <li>{@code sbomer.jboss.org/generation-request}</li>
 * </ul>
 * </p>
 */
@ControllerConfiguration(
        labelSelector = Labels.LABEL_SELECTOR,
        namespaces = { Constants.WATCH_CURRENT_NAMESPACE },
        dependents = {
                @Dependent(
                        name = "init",
                        type = TaskRunInitDependentResource.class,
                        useEventSourceWithName = EVENT_SOURCE_NAME,
                        reconcilePrecondition = NotFinalOrFailedRequestCondition.class,
                        readyPostcondition = InitFinishedCondition.class),
                @Dependent(
                        type = TaskRunGenerateDependentResource.class,
                        dependsOn = "init",
                        useEventSourceWithName = EVENT_SOURCE_NAME) })
@Slf4j
public class GenerationRequestReconciler implements Reconciler<GenerationRequest>,
        EventSourceInitializer<GenerationRequest>, Cleaner<GenerationRequest> {

    public static final String EVENT_SOURCE_NAME = "GenerationRequestEventSource";

    List<TaskRunGenerateDependentResource> generations = new ArrayList<>();

    public GenerationRequestReconciler() {

    }

    @ConfigProperty(name = "sbomer.sbom.sbom-dir")
    String sbomDir;

    @Inject
    SbomRepository sbomRepository;

    @Inject
    KubernetesClient kubernetesClient;

    @Inject
    NotificationService notificationService;

    ObjectMapper objectMapper = ObjectMapperProvider.yaml();

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
     * @param generationRequest
     * @param secondaryResources
     * @return Action to take on the {@link GenerationRequest} resource.
     */
    private UpdateControl<GenerationRequest> reconcileNew(
            GenerationRequest generationRequest,
            Set<TaskRun> secondaryResources) {
        TaskRun initTaskRun = initTaskRun(secondaryResources);

        if (initTaskRun == null) {
            return UpdateControl.noUpdate();
        }

        if (isFinished(initTaskRun)) {
            if (isSuccessful(initTaskRun)) {
                generationRequest.setStatus(SbomGenerationStatus.INITIALIZED);
                setConfig(generationRequest, initTaskRun);
            } else {
                generationRequest.setStatus(SbomGenerationStatus.FAILED);
            }
        } else {
            generationRequest.setStatus(SbomGenerationStatus.INITIALIZING);
        }

        return UpdateControl.updateResource(generationRequest);
    }

    /**
     * Possible next statuses: {@link SbomGenerationStatus#FAILED}, {@link SbomGenerationStatus#INITIALIZED}
     *
     * @param secondaryResources
     * @param generationRequest
     *
     * @return
     */
    private UpdateControl<GenerationRequest> reconcileInitializing(
            GenerationRequest generationRequest,
            Set<TaskRun> secondaryResources) {

        TaskRun initTaskRun = initTaskRun(secondaryResources);

        if (isFinished(initTaskRun)) {
            if (isSuccessful(initTaskRun)) {
                generationRequest.setStatus(SbomGenerationStatus.INITIALIZED);
                setConfig(generationRequest, initTaskRun);
            } else {
                StringBuilder sb = new StringBuilder("Configuration initialization failed. ");

                if (initTaskRun.getStatus() != null && initTaskRun.getStatus().getSteps() != null
                        && !initTaskRun.getStatus().getSteps().isEmpty()
                        && initTaskRun.getStatus().getSteps().get(0).getTerminated() != null) {

                    // At this point the config generation failed, let's try to provide more info on the failure
                    switch (initTaskRun.getStatus().getSteps().get(0).getTerminated().getExitCode()) {
                        case 2:
                            sb.append("Configuration validation failed. ");
                            break;
                        case 3:
                            sb.append("Could not find configuration. ");
                            break;
                        default:
                            sb.append("Unexpected error occurred. ");
                            break;
                    }
                } else {
                    sb.append("System failure. ");
                }

                String reason = sb.append("See logs for more information.").toString();

                log.warn("GenerationRequest '{}' failed. {}", generationRequest.getName(), reason);

                generationRequest.setStatus(SbomGenerationStatus.FAILED);
                generationRequest.setReason(reason);
            }
        } else {
            return UpdateControl.noUpdate();
        }

        return UpdateControl.updateResource(generationRequest);
    }

    /**
     * Possible next statuses: {@link SbomGenerationStatus#GENERATING}
     *
     * @param secondaryResources
     * @param generationRequest
     *
     * @return
     */
    private UpdateControl<GenerationRequest> reconcileInitialized(
            GenerationRequest generationRequest,
            Set<TaskRun> secondaryResources) {

        Set<TaskRun> generateTaskRuns = generateTaskRuns(secondaryResources);

        if (generateTaskRuns.isEmpty()) {
            return UpdateControl.noUpdate();
        }

        generationRequest.setStatus(SbomGenerationStatus.GENERATING);
        return UpdateControl.updateResource(generationRequest);
    }

    /**
     * Possible next statuses: {@link SbomGenerationStatus#FAILED}, {@link SbomGenerationStatus#FINISHED}
     *
     * @param secondaryResources
     * @param generationRequest
     *
     * @return
     */
    private UpdateControl<GenerationRequest> reconcileGenerating(
            GenerationRequest generationRequest,
            Set<TaskRun> secondaryResources) {

        Config config = generationRequest.toConfig();

        if (config == null) {
            log.error(
                    "Product configuration from GenerationRequest '{}' could not be read",
                    generationRequest.getName());
            generationRequest.setStatus(SbomGenerationStatus.FAILED);
            generationRequest.setReason("Could not read product configuration");
            return UpdateControl.updateResource(generationRequest);
        }

        Set<TaskRun> generateTaskRuns = generateTaskRuns(secondaryResources);

        // This should not happen, because if we updated already the status to SbomGenerationStatus.GENERATING, then
        // there was a TaskRun already. But it could be deleted manually(?). In such case we set the status to FAILED.
        if (generateTaskRuns.isEmpty()) {
            log.warn(
                    "Marking GenerationRequest '{}' as failed: no generation TaskRuns were found, but at least one was expected.",
                    generationRequest.getName());

            generationRequest.setStatus(SbomGenerationStatus.FAILED);
            generationRequest.setReason(
                    "Generation failed. Expected one or more running TaskRun related to generation. None found. See logs for more information.");
            return UpdateControl.updateResource(generationRequest);
        }

        log.debug(
                "Found {} TaskRuns related to GenerationRequest '{}'",
                generateTaskRuns.size(),
                generationRequest.getName());

        // Check for still running tasks
        List<TaskRun> stillRunning = generateTaskRuns.stream().filter(tr -> isFinished(tr) == false).toList();

        // If there are tasks that hasn't finished yet, we need to wait.
        if (!stillRunning.isEmpty()) {
            log.debug(
                    "Skipping update of GenerationRequest '{}', because {} TaskRuns are still running: {}",
                    generationRequest.getName(),
                    stillRunning.size(),
                    stillRunning.stream().map(tr -> tr.getMetadata().getName()).toArray());
            return UpdateControl.noUpdate();
        }

        // Get list of failed TaskRuns
        List<TaskRun> failedTaskRuns = generateTaskRuns.stream().filter(tr -> isSuccessful(tr) == false).toList();

        // If there are failed TaskRuns
        if (!failedTaskRuns.isEmpty()) {
            StringBuilder sb = new StringBuilder("Generation failed. ");

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

                    switch (taskRun.getStatus().getSteps().get(0).getTerminated().getExitCode()) {
                        case 2:
                            sb.append("product configuration failure. ");
                            break;
                        case 3:
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
                        case 4:
                            sb.append("an error occurred while generating the SBOM. ");
                            break;
                        default:
                            sb.append("unexpected error occurred. ");
                            break;
                    }
                } else {
                    sb.append("system failure. ");
                }

            }

            String reason = sb.append("See logs for more information.").toString();

            log.warn("Marking GenerationRequest '{}' as failed. Reason: {}", generationRequest.getName(), reason);

            generationRequest.setStatus(SbomGenerationStatus.FAILED);
            generationRequest.setReason(reason);

            return UpdateControl.updateResource(generationRequest);

        }

        storeAndNotify(generationRequest);

        generationRequest.setStatus(SbomGenerationStatus.FINISHED);
        return UpdateControl.updateResource(generationRequest);
    }

    private Param getParamValue(TaskRun taskRun, String paramName) {
        Optional<Param> param = taskRun.getSpec()
                .getParams()
                .stream()
                .filter(p -> p.getName().equals(paramName))
                .findFirst();

        return param.orElse(null);
    }

    private void storeAndNotify(GenerationRequest generationRequest) {
        List<Sbom> sboms = storeSboms(generationRequest);
        notificationService.notifyCompleted(sboms);
    }

    /**
     * Final success status.
     *
     * @param secondaryResources
     * @param generationRequest
     *
     * @return
     */
    private UpdateControl<GenerationRequest> reconcileFinished(
            GenerationRequest generationRequest,
            Set<TaskRun> secondaryResources) {

        // At this point al the work is finished and we can clean up the GenerationRequest Kubernetes resource.
        kubernetesClient.configMaps().withName(generationRequest.getMetadata().getName()).delete();

        return UpdateControl.noUpdate();
    }

    /**
     * Final failed status.
     *
     * @param secondaryResources
     * @param generationRequest
     *
     * @return
     */
    private UpdateControl<GenerationRequest> reconcileFailed(
            GenerationRequest generationRequest,
            Set<TaskRun> secondaryResources) {

        return UpdateControl.noUpdate();
    }

    /**
     * Returns a set of generation-related {@link TaskRun}s from the give {@link TaskRun} {@link Set}.
     *
     * @param taskRuns
     * @return The {@link Set} containing {@link TaskRun} or empty set if not found.
     */
    private Set<TaskRun> generateTaskRuns(Set<TaskRun> taskRuns) {
        return taskRuns.stream().filter(tr -> {
            if (Objects.equals(
                    tr.getMetadata().getLabels().get(Labels.LABEL_PHASE),
                    SbomGenerationPhase.GENERATE.name().toLowerCase())) {
                return true;
            }

            return false;
        }).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * Returns the initialization {@link TaskRun} from the give {@link TaskRun} {@link Set}.
     *
     * @param taskRuns
     * @return The {@link TaskRun} or {@code null} if not found.
     */
    private TaskRun initTaskRun(Set<TaskRun> taskRuns) {
        Optional<TaskRun> taskRun = taskRuns.stream().filter(tr -> {
            if (Objects.equals(
                    tr.getMetadata().getLabels().get(Labels.LABEL_PHASE),
                    SbomGenerationPhase.INIT.name().toLowerCase())) {
                return true;
            }

            return false;
        }).findFirst();

        return taskRun.orElse(null);
    }

    /**
     * Checks whether given {@link TaskRun} has finished successfully.
     *
     * @param taskRun The {@link TaskRun} to check
     * @return {@code true} if the {@link TaskRun} finished successfully, {@code false} otherwise or {@code null} in
     *         case it is still in progress.
     */
    private Boolean isSuccessful(TaskRun taskRun) {
        if (!isFinished(taskRun)) {
            log.trace("TaskRun '{}' still in progress", taskRun.getMetadata().getName());
            return null;
        }

        if (taskRun.getStatus() != null && taskRun.getStatus().getConditions() != null
                && taskRun.getStatus().getConditions().size() > 0
                && Objects.equals(taskRun.getStatus().getConditions().get(0).getStatus(), "True")) {
            log.trace("TaskRun '{}' finished successfully", taskRun.getMetadata().getName());
            return true;
        }

        log.trace("TaskRun '{}' failed", taskRun.getMetadata().getName());
        return false;
    }

    /**
     * Checks whether given {@link TaskRun} has finished or not.
     *
     * @param taskRun The {@link TaskRun} to check
     * @return {@code true} if the {@link TaskRun} finished, {@code false} otherwise
     */
    private boolean isFinished(TaskRun taskRun) {
        if (taskRun.getStatus() != null && taskRun.getStatus().getConditions() != null
                && taskRun.getStatus().getConditions().size() > 0
                && (Objects.equals(taskRun.getStatus().getConditions().get(0).getStatus(), "True")
                        || Objects.equals(taskRun.getStatus().getConditions().get(0).getStatus(), "False"))) {

            log.trace("TaskRun '{}' finished", taskRun.getMetadata().getName());
            return true;
        }

        log.trace("TaskRun '{}' still running", taskRun.getMetadata().getName());
        return false;
    }

    @Override
    @Transactional
    public UpdateControl<GenerationRequest> reconcile(
            GenerationRequest generationRequest,
            Context<GenerationRequest> context) throws Exception {

        MDCUtils.removeContext();
        MDCUtils.addBuildContext(generationRequest.getBuildId());

        // No status set set, it should be "NEW", let's do it.
        // "NEW" starts everything.
        if (Objects.isNull(generationRequest.getStatus())) {
            generationRequest.setStatus(SbomGenerationStatus.NEW);
            return UpdateControl.updateResource(generationRequest);
        }

        // Fetch any secondary resources (Tekton TaskRuns) that are related to the primary resource (GenerationRequest)
        // There may be between 0 or more TaskRuns related to the GenerationRequest:
        // 0 In case these were not created yet,
        // 1 in case the initiation tasks is only running
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
                action = reconcileFinished(generationRequest, secondaryResources);
                break;
            case FAILED:
                action = reconcileFailed(generationRequest, secondaryResources);
                break;
            default:
                break;
        }

        // This would be unexpected.
        if (action == null) {
            log.error(
                    "Unknown status received: '{}'' for GenerationRequest '{}",
                    generationRequest.getStatus(),
                    generationRequest.getMetadata().getName());
            return UpdateControl.noUpdate();
        }

        // In case resource gets an update, update th DB entity as well
        if (action.isUpdateResource()) {
            sync(generationRequest);
        }

        return action;
    }

    protected SbomGenerationRequest sync(GenerationRequest generationRequest) {
        return SbomGenerationRequest.sync(generationRequest);
    }

    protected List<Sbom> storeSboms(GenerationRequest generationRequest) {
        SbomGenerationRequest sbomGenerationRequest = sync(generationRequest);

        log.info(
                "Reading all generated SBOMs for the GenerationRequest '{}'",
                generationRequest.getMetadata().getName());

        Config config = SbomUtils.fromJsonConfig(sbomGenerationRequest.getConfig());

        List<Sbom> sboms = new ArrayList<>();

        for (int i = 0; i < config.getProducts().size(); i++) {
            log.info("Reading SBOM for index '{}'", i);

            Path sbomPath = Path.of(
                    sbomDir,
                    generationRequest.getMetadata().getName(),
                    generationRequest.getMetadata().getName() + "-1-generate-" + i,
                    "bom.json"); // TODO: should not be hardcoded

            // Read the generated SBOM JSON file
            Bom bom = SbomUtils.fromPath(sbomPath);

            // Create the Sbom entity
            Sbom sbom = Sbom.builder()
                    .withId(RandomStringIdGenerator.generate())
                    .withBuildId(generationRequest.getBuildId())
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
        Config config;

        try {
            config = objectMapper.readValue(configVal.getBytes(), Config.class);
        } catch (IOException e) {
            throw new ApplicationException(
                    "Could not parse the '{}' result within the TaskRun '{}': {}",
                    TaskRunInitDependentResource.RESULT_NAME,
                    taskRun.getMetadata().getName(),
                    configVal);
        }

        log.debug("Runtime config from TaskRun '{}' parsed: {}", taskRun.getMetadata().getName(), config);

        try {
            generationRequest.setConfig(objectMapper.writeValueAsString(config));
        } catch (JsonProcessingException e) {
            log.error("Unable to serialize product configuration", e);
        }

        return config;
    }

    @Override
    public Map<String, EventSource> prepareEventSources(EventSourceContext<GenerationRequest> context) {
        InformerEventSource<TaskRun, GenerationRequest> ies = new InformerEventSource<>(
                InformerConfiguration.from(TaskRun.class, context)
                        .withNamespacesInheritedFromController(context)
                        .build(),
                context);

        return Map.of(EVENT_SOURCE_NAME, ies);
    }

    @Override
    public DeleteControl cleanup(GenerationRequest resource, Context<GenerationRequest> context) {
        log.debug("GenerationRequest '{}' was removed from the system", resource.getMetadata().getName());
        return DeleteControl.defaultDelete();
    }

}
