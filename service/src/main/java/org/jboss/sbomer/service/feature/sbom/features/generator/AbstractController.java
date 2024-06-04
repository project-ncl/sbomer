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
package org.jboss.sbomer.service.feature.sbom.features.generator;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.cyclonedx.model.Bom;
import org.jboss.sbomer.core.features.sbom.enums.GenerationResult;
import org.jboss.sbomer.core.features.sbom.utils.MDCUtils;
import org.jboss.sbomer.core.features.sbom.utils.SbomUtils;
import org.jboss.sbomer.service.feature.sbom.config.GenerationRequestControllerConfig;
import org.jboss.sbomer.service.feature.sbom.k8s.model.GenerationRequest;
import org.jboss.sbomer.service.feature.sbom.k8s.model.SbomGenerationPhase;
import org.jboss.sbomer.service.feature.sbom.k8s.model.SbomGenerationStatus;
import org.jboss.sbomer.service.feature.sbom.k8s.resources.Labels;
import org.jboss.sbomer.service.feature.sbom.model.RandomStringIdGenerator;
import org.jboss.sbomer.service.feature.sbom.model.Sbom;
import org.jboss.sbomer.service.feature.sbom.model.SbomGenerationRequest;
import org.jboss.sbomer.service.feature.sbom.service.SbomRepository;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.tekton.pipeline.v1beta1.TaskRun;
import io.javaoperatorsdk.operator.api.config.informer.InformerConfiguration;
import io.javaoperatorsdk.operator.api.reconciler.Cleaner;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.DeleteControl;
import io.javaoperatorsdk.operator.api.reconciler.EventSourceContext;
import io.javaoperatorsdk.operator.api.reconciler.EventSourceInitializer;
import io.javaoperatorsdk.operator.api.reconciler.Reconciler;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;
import io.javaoperatorsdk.operator.processing.event.source.EventSource;
import io.javaoperatorsdk.operator.processing.event.source.informer.InformerEventSource;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractController implements Reconciler<GenerationRequest>,
        EventSourceInitializer<GenerationRequest>, Cleaner<GenerationRequest> {

    public static final String EVENT_SOURCE_NAME = "GenerationRequestEventSource";

    @Inject
    SbomRepository sbomRepository;

    @Inject
    protected GenerationRequestControllerConfig controllerConfig;

    @Inject
    KubernetesClient kubernetesClient;

    protected abstract UpdateControl<GenerationRequest> updateRequest(
            GenerationRequest generationRequest,
            SbomGenerationStatus status,
            GenerationResult result,
            String reason);

    /**
     * Returns the {@link TaskRun} having the specified {@link SbomGenerationPhase} from the given {@link TaskRun}
     * {@link Set}.
     *
     * @param taskRuns
     * @param phase
     * @return The {@link TaskRun} or {@code null} if not found.
     */
    protected TaskRun findTaskRun(Set<TaskRun> taskRuns, SbomGenerationPhase phase) {
        Optional<TaskRun> taskRun = findTaskRuns(taskRuns, phase).stream().findFirst();

        return taskRun.orElse(null);
    }

    /**
     * Returns a set of {@link TaskRun}s having the specified {@link SbomGenerationPhase} from the given {@link TaskRun}
     * {@link Set}.
     *
     * @param taskRuns
     * @param phase
     * @return The {@link Set} containing {@link TaskRun} or empty set if not found.
     */
    protected Set<TaskRun> findTaskRuns(Set<TaskRun> taskRuns, SbomGenerationPhase phase) {
        return taskRuns.stream().filter(tr -> {
            if (Objects.equals(tr.getMetadata().getLabels().get(Labels.LABEL_PHASE), phase.name().toLowerCase())) {
                return true;
            }

            return false;
        }).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * <p>
     * Stores the generated manifest in the database which results in creation of a new {@link Sbom} entity.
     * </p>
     *
     * <p>
     * Additionally updates the database entity with the current state of the {@link GenerationRequest}.
     * </p>
     *
     * @param generationRequest
     * @param bom
     * @return
     */
    @Transactional
    protected Sbom storeSbom(GenerationRequest generationRequest, Bom bom) {
        // First, update the status of the GenerationRequest entity.
        SbomGenerationRequest sbomGenerationRequest = SbomGenerationRequest.sync(generationRequest);

        log.info(
                "Storing generated manifest for the GenerationRequest '{}'",
                generationRequest.getMetadata().getName());

        // Create the Sbom entity
        Sbom sbom = Sbom.builder()
                .withId(RandomStringIdGenerator.generate())
                .withIdentifier(generationRequest.getIdentifier())
                .withSbom(SbomUtils.toJsonNode(bom))
                .withGenerationRequest(sbomGenerationRequest)
                .build();

        // And store it in the database
        return sbomRepository.saveSbom(sbom);
    }

    /**
     * Checks whether given {@link TaskRun} has finished or not.
     *
     * @param taskRun The {@link TaskRun} to check
     * @return {@code true} if the {@link TaskRun} finished, {@code false} otherwise
     */
    protected boolean isFinished(TaskRun taskRun) {
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

    /**
     * Checks whether given {@link TaskRun} has finished successfully.
     *
     * @param taskRun The {@link TaskRun} to check
     * @return {@code true} if the {@link TaskRun} finished successfully, {@code false} otherwise or {@code null} in
     *         case it is still in progress.
     */
    protected Boolean isSuccessful(TaskRun taskRun) {
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

    @Override
    public UpdateControl<GenerationRequest> reconcile(
            GenerationRequest generationRequest,
            Context<GenerationRequest> context) throws Exception {
        MDCUtils.removeContext();

        // No status set set, it should be "NEW", let's do it.
        // "NEW" starts everything.
        if (Objects.isNull(generationRequest.getStatus())) {
            return updateRequest(generationRequest, SbomGenerationStatus.NEW, null, null);
        }

        Set<TaskRun> secondaryResources = context.getSecondaryResources(TaskRun.class);

        UpdateControl<GenerationRequest> action = null;

        log.debug(
                "Handling update for GenerationRequest '{}', current status: '{}'",
                generationRequest.getMetadata().getName(),
                generationRequest.getStatus());

        MDCUtils.addBuildContext(generationRequest.getIdentifier());

        switch (generationRequest.getStatus()) {
            case NEW:
                action = reconcileNew(generationRequest, secondaryResources);
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
            SbomGenerationRequest.sync(generationRequest);
        }

        return action;
    }

    /**
     * <p>
     * In case the dependent resource is prepared, the {@link SbomGenerationStatus#NEW} status is transitioned into
     * {@link SbomGenerationStatus#GENERATING}. In all other cases, the resourse stays in the
     * {@link SbomGenerationStatus#NEW} status.
     * </p>
     */
    protected UpdateControl<GenerationRequest> reconcileNew(
            GenerationRequest generationRequest,
            Set<TaskRun> secondaryResources) {
        log.debug("Reconcile NEW for '{}'...", generationRequest.getName());

        TaskRun generateTaskRun = findTaskRun(secondaryResources, SbomGenerationPhase.GENERATE);

        if (generateTaskRun == null) {
            return UpdateControl.noUpdate();
        }

        return updateRequest(generationRequest, SbomGenerationStatus.GENERATING, null, null);
    }

    /**
     * <p>
     * Handling of failed generation.
     * </p>
     *
     * @param generationRequest
     * @param secondaryResources
     * @return
     */
    protected UpdateControl<GenerationRequest> reconcileFailed(
            GenerationRequest generationRequest,
            Set<TaskRun> secondaryResources) {
        log.debug("Reconcile FAILED for '{}'...", generationRequest.getName());

        // In case the generation request failed, we need to clean up resources so that these are not left forever.
        // We have all the data elsewhere (logs, cause) so it's safe to do so.
        cleanupFinishedGenerationRequest(generationRequest);

        return UpdateControl.noUpdate();
    }

    /**
     * <p>
     * Handles finished generation.
     * </p>
     *
     * @param generationRequest
     * @param secondaryResources
     * @return
     */
    protected UpdateControl<GenerationRequest> reconcileFinished(
            GenerationRequest generationRequest,
            Set<TaskRun> secondaryResources) {

        log.debug("Reconcile FINISHED for '{}'...", generationRequest.getName());

        // notificationService.notifyCompleted(sboms);
        // At this point al the work is finished and we can clean up the GenerationRequest Kubernetes resource.
        cleanupFinishedGenerationRequest(generationRequest);

        return UpdateControl.noUpdate();
    }

    /**
     * <p>
     * Handles updates to {@link GenerationRequest} being in progress.
     * </p>
     *
     * @param generationRequest
     * @param secondaryResources
     * @return
     */
    @Transactional
    protected abstract UpdateControl<GenerationRequest> reconcileGenerating(
            GenerationRequest generationRequest,
            Set<TaskRun> secondaryResources);

    /**
     * Removes related to finished {@link GenerationRequest} and its instance as well.
     *
     * @param generationRequest
     */
    private void cleanupFinishedGenerationRequest(GenerationRequest generationRequest) {
        if (!controllerConfig.cleanup()) {
            log.debug(
                    "The cleanup setting is set to false, skipping cleaning up finished GenerationRequest '{}'",
                    generationRequest.getName());
            return;
        }

        Path workdirPath = Path.of(controllerConfig.sbomDir(), generationRequest.getMetadata().getName());

        log.debug(
                "Removing '{}' path being the working directory for the finished '{}' GenerationRequest",
                workdirPath.toAbsolutePath().toString(),
                generationRequest.getName());

        // It should, but...
        if (Files.exists(workdirPath)) {
            try {
                Files.walk(workdirPath).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
            } catch (IOException e) {
                log.error(
                        "An error occurred while removing the '{}' directory",
                        workdirPath.toAbsolutePath().toString(),
                        e);
            }
        }

        if (Files.exists(workdirPath)) {
            log.warn("Directory '{}' still exists", workdirPath.toAbsolutePath().toString());
        } else {
            log.debug("Directory '{}' removed", workdirPath.toAbsolutePath().toString());
        }

        kubernetesClient.configMaps().withName(generationRequest.getMetadata().getName()).delete();
    }
}
