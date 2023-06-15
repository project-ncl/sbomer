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
package org.jboss.sbomer.feature.sbom.k8s.reconciler;

import static org.jboss.sbomer.feature.sbom.k8s.reconciler.GenerationRequestReconciler.EVENT_SOURCE_NAME;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.jboss.sbomer.core.errors.ApplicationException;
import org.jboss.sbomer.core.utils.MDCUtils;
import org.jboss.sbomer.feature.sbom.core.config.ConfigReader;
import org.jboss.sbomer.feature.sbom.core.config.runtime.Config;
import org.jboss.sbomer.feature.sbom.k8s.model.GenerationRequest;
import org.jboss.sbomer.feature.sbom.k8s.model.SbomGenerationPhase;
import org.jboss.sbomer.feature.sbom.k8s.model.SbomGenerationStatus;
import org.jboss.sbomer.feature.sbom.k8s.reconciler.condition.InitFinishedCondition;
import org.jboss.sbomer.feature.sbom.k8s.reconciler.condition.NewOrFailedRequestCondition;
import org.jboss.sbomer.feature.sbom.k8s.resources.Labels;
import org.jboss.sbomer.feature.sbom.k8s.resources.TaskRunGenerateDependentResource;
import org.jboss.sbomer.feature.sbom.k8s.resources.TaskRunInitDependentResource;

import com.fasterxml.jackson.core.JsonProcessingException;

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
                        reconcilePrecondition = NewOrFailedRequestCondition.class,
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

    @Inject
    ConfigReader configReader;

    @Override
    public UpdateControl<GenerationRequest> reconcile(
            GenerationRequest generationRequest,
            Context<GenerationRequest> context) throws Exception {

        MDCUtils.removeContext();
        MDCUtils.addBuildContext(generationRequest.getBuildId());

        // Fetch any secondary resources (Tekton TaskRuns) that are related to the primary resource (GenerationRequest)
        // There may be between 0 and to 2 TaskRuns related to the GenerationRequest
        Set<TaskRun> secondaryResources = context.getSecondaryResources(TaskRun.class);

        SbomGenerationStatus currentStatus = generationRequest.getStatus();

        // Fetch latest TaskRun in the workflow and use this information to handle the change.
        // Any TaskRuns that are run earlier the workflow are ignored, because these have finished already.
        Optional<TaskRun> taskRunOpt = secondaryResources.stream()
                .sorted((i1, i2) -> i2.getMetadata().getName().compareTo(i1.getMetadata().getName()))
                .findFirst();

        // In case there is no TaskRun found, no update needed.
        if (taskRunOpt.isEmpty()) {
            return UpdateControl.noUpdate();
        }

        // Handle the related TaskRun and reflect it in the GenerationRequest status.
        SbomGenerationStatus desiredStatus = handleRelatedTaskRunUpdate(generationRequest, taskRunOpt.get());

        // No update needed (yet)
        if (desiredStatus == null) {
            return UpdateControl.noUpdate();
        }

        log.debug("Desired status: '{}', current status: '{}'", desiredStatus, currentStatus);

        // If the desired status is newer than what we already have update the GenerationRequest with it.
        if (currentStatus == null || currentStatus.isOlderThan(desiredStatus)) {

            log.info(
                    "Updating status of GenerationRequest '{}': from '{}' to '{}'",
                    generationRequest.getMetadata().getName(),
                    currentStatus,
                    desiredStatus);

            generationRequest.setStatus(desiredStatus);

            return UpdateControl.updateResource(generationRequest);

        }

        return UpdateControl.noUpdate();
    }

    private SbomGenerationStatus handleRelatedTaskRunUpdate(GenerationRequest generationRequest, TaskRun taskRun) {

        // No status subresource yet, wait.
        if (taskRun.getStatus() == null) {
            return null;
        }

        log.debug(
                "Handling TaskRun: '{}' related to GenerationRequest '{}' from",
                taskRun.getMetadata().getName(),
                generationRequest.getMetadata().getName());

        String phaseLabelValue = taskRun.getMetadata().getLabels().get(Labels.LABEL_PHASE);

        if (phaseLabelValue == null) {
            log.error(
                    "The TaskRun '{}' does not have the expected label '{}' set",
                    taskRun.getMetadata().getName(),
                    Labels.LABEL_PHASE);

            return SbomGenerationStatus.FAILED;
        }

        SbomGenerationPhase phase = SbomGenerationPhase.valueOf(phaseLabelValue.toUpperCase());

        log.debug(
                "Handling GenerationRequest '{}' TaskRun '{}' update for phase '{}' ",
                generationRequest.getMetadata().getName(),
                taskRun.getMetadata().getName(),
                phase);

        switch (phase) {
            case INIT:
                return handleInitTaskRunUpdate(generationRequest, taskRun);
            case GENERATE:
                return handleGenerateTaskRunUpdate(generationRequest, taskRun);

        }

        return null;
    }

    private SbomGenerationStatus handleInitTaskRunUpdate(GenerationRequest generationRequest, TaskRun taskRun) {
        switch (taskRun.getStatus().getConditions().get(0).getStatus()) {
            case "Unknown":
                return SbomGenerationStatus.INITIALIZING;
            case "True":
                log.info("Initialization finished");

                handleConfigResult(generationRequest, taskRun);

                // TODO: fetch result (runtime configuration) and store it
                return SbomGenerationStatus.INITIALIZED;
            case "False":
                // TODO: get failure reason
                // TODO for how long should we leave failed requests? when to do cleanup?
                return SbomGenerationStatus.FAILED;
        }

        return null;
    }

    private Config handleConfigResult(GenerationRequest generationRequest, TaskRun taskRun) {
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
            config = configReader.getYamlObjectMapper().readValue(configVal.getBytes(), Config.class);
        } catch (IOException e) {
            throw new ApplicationException(
                    "Could not parse the '{}' result within the TaskRun '{}': {}",
                    TaskRunInitDependentResource.RESULT_NAME,
                    taskRun.getMetadata().getName(),
                    configVal);
        }

        log.debug("Runtime config from TaskRun '{}' parsed: {}", taskRun.getMetadata().getName(), config);

        try {
            generationRequest.setConfig(configReader.getYamlObjectMapper().writeValueAsString(config));
        } catch (JsonProcessingException e) {
            log.error("Unable to serialize product configuration", e);
        }

        return config;
    }

    private SbomGenerationStatus handleGenerateTaskRunUpdate(GenerationRequest generationRequest, TaskRun taskRun) {
        switch (taskRun.getStatus().getConditions().get(0).getStatus()) {
            case "Unknown":
                return SbomGenerationStatus.GENERATING;
            case "True":
                // TODO: handle SBOM entity creation
                return SbomGenerationStatus.FINISHED;
            case "False":
                // TODO: get failure reason
                // TODO for how long should we leave failed requests? when to do cleanup?
                return SbomGenerationStatus.FAILED;
        }

        return null;
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
        log.debug("Removed!");

        return DeleteControl.defaultDelete();
    }
}
