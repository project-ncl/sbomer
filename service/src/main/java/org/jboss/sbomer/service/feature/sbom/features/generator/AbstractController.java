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

import static org.jboss.sbomer.core.features.sbom.utils.SbomUtils.addPropertyIfMissing;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.cyclonedx.model.Bom;
import org.jboss.sbomer.core.config.request.ErrataAdvisoryRequestConfig;
import org.jboss.sbomer.core.errors.ApplicationException;
import org.jboss.sbomer.core.features.sbom.Constants;
import org.jboss.sbomer.core.features.sbom.enums.GenerationRequestType;
import org.jboss.sbomer.core.features.sbom.enums.GenerationResult;
import org.jboss.sbomer.core.features.sbom.utils.MDCUtils;
import org.jboss.sbomer.core.features.sbom.utils.SbomUtils;
import org.jboss.sbomer.service.feature.errors.FeatureDisabledException;
import org.jboss.sbomer.service.feature.s3.S3StorageHandler;
import org.jboss.sbomer.service.feature.sbom.atlas.AtlasHandler;
import org.jboss.sbomer.service.feature.sbom.config.GenerationRequestControllerConfig;
import org.jboss.sbomer.service.feature.sbom.features.umb.producer.NotificationService;
import org.jboss.sbomer.service.feature.sbom.k8s.model.GenerationRequest;
import org.jboss.sbomer.service.feature.sbom.k8s.model.SbomGenerationPhase;
import org.jboss.sbomer.service.feature.sbom.k8s.model.SbomGenerationStatus;
import org.jboss.sbomer.service.feature.sbom.k8s.reconciler.TektonExitCodeUtils;
import org.jboss.sbomer.service.feature.sbom.k8s.resources.Labels;
import org.jboss.sbomer.service.feature.sbom.model.RandomStringIdGenerator;
import org.jboss.sbomer.service.feature.sbom.model.RequestEvent;
import org.jboss.sbomer.service.feature.sbom.model.Sbom;
import org.jboss.sbomer.service.feature.sbom.model.SbomGenerationRequest;
import org.jboss.sbomer.service.feature.sbom.service.SbomRepository;
import org.slf4j.helpers.MessageFormatter;

import io.fabric8.knative.pkg.apis.Condition;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.tekton.v1beta1.TaskRun;
import io.fabric8.tekton.v1beta1.TaskRunStatus;
import io.javaoperatorsdk.operator.api.config.informer.InformerEventSourceConfiguration;
import io.javaoperatorsdk.operator.api.reconciler.Cleaner;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.DeleteControl;
import io.javaoperatorsdk.operator.api.reconciler.EventSourceContext;
import io.javaoperatorsdk.operator.api.reconciler.Reconciler;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;
import io.javaoperatorsdk.operator.processing.event.source.EventSource;
import io.javaoperatorsdk.operator.processing.event.source.informer.InformerEventSource;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractController implements Reconciler<GenerationRequest>, Cleaner<GenerationRequest> {

    @Inject
    @Setter
    protected SbomRepository sbomRepository;

    @Inject
    @Setter
    protected GenerationRequestControllerConfig controllerConfig;

    @Inject
    KubernetesClient kubernetesClient;

    @Inject
    @Setter
    NotificationService notificationService;

    @Inject
    S3StorageHandler s3LogHandler;

    @Inject
    @Setter
    AtlasHandler atlasHandler;

    protected abstract GenerationRequestType generationRequestType();

    private String labelSelector() {
        return Labels.defaultLabelsToMap(generationRequestType())
                .entrySet()
                .stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining(","));
    }

    @Override
    public List<EventSource<?, GenerationRequest>> prepareEventSources(EventSourceContext<GenerationRequest> context) {
        String eventSourceName = "tekton-generation-request-" + generationRequestType().toName();

        log.info(
                "Preparing event source '{}' to handle generation requests with type '{}'...",
                eventSourceName,
                generationRequestType());

        InformerEventSource<TaskRun, GenerationRequest> ies = new InformerEventSource<>(
                InformerEventSourceConfiguration.from(TaskRun.class, GenerationRequest.class)
                        .withName(eventSourceName)
                        .withLabelSelector(labelSelector())
                        .withNamespacesInheritedFromController()
                        .withFollowControllerNamespacesChanges(true)
                        .build(),
                context);

        log.info("Event source '{}' prepared", eventSourceName);

        return List.of(ies);
    }

    protected void setPhaseLabel(GenerationRequest generationRequest) {
        // Default: do nothing; subclasses can override this
    }

    protected UpdateControl<GenerationRequest> updateRequest(
            GenerationRequest generationRequest,
            SbomGenerationStatus status,
            GenerationResult result,
            String reason,
            Object... params) {

        setPhaseLabel(generationRequest);

        generationRequest.setStatus(status);
        generationRequest.setResult(result);
        generationRequest.setReason(MessageFormatter.arrayFormat(reason, params).getMessage());

        return UpdateControl.patchResource(generationRequest);
    }

    /**
     * Returns the {@link TaskRun} having the specified {@link SbomGenerationPhase} from the given {@link TaskRun}
     * {@link Set}.
     *
     * @param taskRuns the task runs
     * @param phase the phase
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
     * @param taskRuns the task runs
     * @param phase the phase
     * @return The {@link Set} containing {@link TaskRun} or empty set if not found.
     */
    protected Set<TaskRun> findTaskRuns(Set<TaskRun> taskRuns, SbomGenerationPhase phase) {
        return taskRuns.stream()
                .filter(
                        tr -> Objects.equals(
                                tr.getMetadata().getLabels().get(Labels.LABEL_PHASE),
                                phase.name().toLowerCase())

                )
                .collect(Collectors.toCollection(LinkedHashSet::new));
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
     * @param generationRequest the generation request
     * @param boms the BOMs to store
     * @return the list of stored {@link Sbom}s
     */
    @Transactional
    protected List<Sbom> storeBoms(GenerationRequest generationRequest, List<Bom> boms) {
        MDCUtils.removeOtelContext();
        MDCUtils.addIdentifierContext(generationRequest.getIdentifier());
        MDCUtils.addOtelContext(generationRequest.getMDCOtel());

        // First, update the status of the GenerationRequest entity.
        SbomGenerationRequest sbomGenerationRequest = SbomGenerationRequest.sync(generationRequest);

        // Verify if the request event for this generation is associated with an Errata advisory
        RequestEvent event = sbomGenerationRequest.getRequest();
        if (event != null && event.getRequestConfig() != null
                && event.getRequestConfig() instanceof ErrataAdvisoryRequestConfig config) {

            boms.forEach(bom -> {
                // Add the AdvisoryId property
                addPropertyIfMissing(
                        bom.getMetadata(),
                        Constants.CONTAINER_PROPERTY_ADVISORY_ID,
                        config.getAdvisoryId());
            });
        }

        log.info("There are {} manifests to be stored for the {} request...", boms.size(), generationRequest.getId());

        List<Sbom> sboms = new ArrayList<>();

        // Create Sboms entities for all manifests
        boms.forEach(
                bom -> sboms.add(
                        Sbom.builder()
                                .withId(RandomStringIdGenerator.generate())
                                .withIdentifier(generationRequest.getIdentifier())
                                .withSbom(SbomUtils.toJsonNode(bom))
                                .withGenerationRequest(sbomGenerationRequest)
                                .build()));

        log.info(
                "Storing {} manifests for the GenerationRequest '{}'",
                boms.size(),
                generationRequest.getMetadata().getName());

        // And store it in the database
        return sbomRepository.saveSboms(sboms);
    }

    /**
     * Checks whether given {@link TaskRun} has finished or not.
     *
     * @param taskRun The {@link TaskRun} to check
     * @return {@code true} if the {@link TaskRun} finished, {@code false} otherwise
     */
    protected boolean isFinished(TaskRun taskRun) {
        if (taskRun.getStatus() != null && taskRun.getStatus().getConditions() != null
                && !taskRun.getStatus().getConditions().isEmpty()
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
            return null; // FIXME: This is not really binary, but trinary state
        }

        TaskRunStatus status = taskRun.getStatus();
        if (status != null && status.getConditions() != null && !status.getConditions().isEmpty()) {
            Condition condition = status.getConditions().get(0);

            String taskRunName = taskRun.getMetadata().getName();
            String conditionStatus = condition.getStatus();
            String reason = condition.getReason();
            String message = condition.getMessage();
            String podName = status.getPodName();

            if (Objects.equals(conditionStatus, "True")) {
                log.trace("TaskRun '{}' finished successfully (Reason: '{}')", taskRunName, reason);
                return true;
            } else {
                log.warn(
                        "TaskRun '{}' failed (Reason: '{}', Message: '{}', Pod: '{}')",
                        taskRunName,
                        reason,
                        message,
                        podName);

                if (status.getSteps() != null) {
                    status.getSteps().forEach(step -> {
                        var term = step.getTerminated();
                        if (term != null) {
                            String exitCodeReason = TektonExitCodeUtils.interpretExitCode(term.getExitCode());
                            log.warn(
                                    "  Step '{}': ExitCode={} ({}), Reason={}, Message={}",
                                    step.getName(),
                                    term.getExitCode(),
                                    exitCodeReason,
                                    term.getReason(),
                                    term.getMessage());
                        }
                    });
                }
            }
        }

        log.trace("TaskRun '{}' failed", taskRun.getMetadata().getName());
        return false;
    }

    protected String getDetailedFailureMessage(TaskRun taskRun) {
        if (taskRun.getStatus() == null || taskRun.getStatus().getSteps() == null) {
            return "TaskRun failed with no step information available.";
        }

        return taskRun.getStatus().getSteps().stream().map(step -> {
            var term = step.getTerminated();
            if (term != null) {
                boolean isOomKilled = "OOMKilled".equals(term.getReason());
                boolean isFailedExit = term.getExitCode() != 0;

                if (isFailedExit || isOomKilled) {
                    String reason = TektonExitCodeUtils.interpretExitCode(term.getExitCode());
                    return String.format(
                            "Step '%s' failed: exitCode=%d (%s), reason=%s%s",
                            step.getName(),
                            term.getExitCode(),
                            reason,
                            term.getReason(),
                            term.getMessage() != null ? (", message=" + term.getMessage()) : " ");
                }
             }

            return null;
        }).filter(Objects::nonNull).findFirst().orElse("TaskRun failed, but no step had non-zero exit code or OOMKilled reason.");
    }

    @Override
    public DeleteControl cleanup(GenerationRequest resource, Context<GenerationRequest> context) {
        MDCUtils.removeOtelContext();
        MDCUtils.addIdentifierContext(resource.getIdentifier());
        MDCUtils.addOtelContext(resource.getMDCOtel());

        log.debug("GenerationRequest '{}' was removed from the system", resource.getMetadata().getName());
        return DeleteControl.defaultDelete();
    }

    @Override
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
            log.error(
                    "Unknown status received: '{}'' for GenerationRequest '{}",
                    generationRequest.getStatus(),
                    generationRequest.getMetadata().getName());
            return UpdateControl.noUpdate();
        }

        // In case resource gets an update, update th DB entity as well
        if (action.isPatchResource()) {
            SbomGenerationRequest.sync(generationRequest);
        }

        return action;
    }

    /**
     * <p>
     * Simply change the status of all resurces with {@link SbomGenerationStatus#NEW} status to
     * {@link SbomGenerationStatus#SCHEDULED}. This is done for backwards-compatibility.
     * </p>
     */
    protected UpdateControl<GenerationRequest> reconcileNew(
            GenerationRequest generationRequest,
            Set<TaskRun> secondaryResources) {
        log.debug("Reconcile NEW for '{}'...", generationRequest.getName());

        return updateRequest(generationRequest, SbomGenerationStatus.SCHEDULED, null, null);
    }

    /**
     * <p>
     * In case the dependent resource is prepared, the {@link SbomGenerationStatus#NEW} status is transitioned into
     * {@link SbomGenerationStatus#GENERATING}. In all other cases, the resource stays in the
     * {@link SbomGenerationStatus#NEW} status.
     * </p>
     */
    protected UpdateControl<GenerationRequest> reconcileScheduled(
            GenerationRequest generationRequest,
            Set<TaskRun> secondaryResources) {
        log.debug("Reconcile SCHEDULED for '{}'...", generationRequest.getName());

        TaskRun generateTaskRun = findTaskRun(secondaryResources, SbomGenerationPhase.GENERATE);

        if (generateTaskRun == null) {
            return UpdateControl.noUpdate();
        }

        return updateRequest(generationRequest, SbomGenerationStatus.GENERATING, null, null);
    }

    /**
     * Handling of failed generation.
     *
     * @param generationRequest the generation request
     * @return the update control for the generation request
     */
    protected UpdateControl<GenerationRequest> reconcileFailed(GenerationRequest generationRequest) {
        log.debug("Reconcile FAILED for '{}'...", generationRequest.getName());

        s3LogHandler.storeFiles(generationRequest);

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
     * @param generationRequest the generation request
     * @return the update control for the generation request
     */
    @ActivateRequestContext
    protected UpdateControl<GenerationRequest> reconcileFinished(GenerationRequest generationRequest) {
        log.debug("Reconcile FINISHED for '{}'...", generationRequest.getName());

        // Store files in S3
        try {
            s3LogHandler.storeFiles(generationRequest);
        } catch (Exception e) {
            // This is not fatal
            log.warn("Storing files in S3 failed", e);
        }

        // We're good, remove all files now!
        cleanupFinishedGenerationRequest(generationRequest);

        return UpdateControl.noUpdate();
    }

    protected void performPost(List<Sbom> sboms) {
        CompletableFuture<Void> publishToUmb = CompletableFuture.runAsync(() -> {
            try {
                notificationService.notifyCompleted(sboms);
            } catch (FeatureDisabledException e) {
                log.warn(e.getMessage(), e);
            }
        }).exceptionally(e -> {
            throw new ApplicationException("UMB notification failed: {}", e.getMessage(), e);
        });

        CompletableFuture<Void> uploadToAtlas = CompletableFuture.runAsync(() -> {
            try {
                atlasHandler.publishBuildManifests(sboms);
            } catch (FeatureDisabledException e) {
                log.warn(e.getMessage(), e);
            }
        }).exceptionally(e -> {
            throw new ApplicationException("Atlas upload failed: {}", e.getMessage(), e);
        });

        try {
            // Wait for all tasks to be done
            CompletableFuture.allOf(publishToUmb, uploadToAtlas).join();
        } catch (CompletionException e) {
            throw new ApplicationException(
                    "Manifest was generated successfully, but at least one of the post-generation tasks did not finish successfully: {}",
                    e.getMessage(),
                    e);
        }
    }

    /**
     * <p>
     * Handles updates to {@link GenerationRequest} being in progress.
     * </p>
     *
     * @param generationRequest the generation request
     * @param secondaryResources the secondary resources
     * @return the update control for the generation request
     */
    @Transactional
    protected abstract UpdateControl<GenerationRequest> reconcileGenerating(
            GenerationRequest generationRequest,
            Set<TaskRun> secondaryResources);

    /**
     * Removes related to finished {@link GenerationRequest} and its instance as well.
     *
     * @param generationRequest the generation request
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
                workdirPath.toAbsolutePath(),
                generationRequest.getName());

        // It should, but...
        if (Files.exists(workdirPath)) {
            try (Stream<Path> stream = Files.walk(workdirPath)) {
                stream.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
            } catch (IOException e) {
                log.error("An error occurred while removing the '{}' directory", workdirPath.toAbsolutePath(), e);
            }
        }

        if (Files.exists(workdirPath)) {
            log.warn("Directory '{}' still exists", workdirPath.toAbsolutePath());
        } else {
            log.debug("Directory '{}' removed", workdirPath.toAbsolutePath());
        }

        kubernetesClient.configMaps().withName(generationRequest.getMetadata().getName()).delete();
    }

    /**
     * Reads manifests for given {@code manifestPaths} and converts them into {@link Bom}s.
     *
     * @param manifestPaths List of {@link Path}s to manifests in JSON format.
     * @return List of {@link Bom}s.
     */
    protected List<Bom> readManifests(List<Path> manifestPaths) {
        List<Bom> boms = new ArrayList<>();

        log.info("Reading {} manifests...", manifestPaths.size());

        for (Path manifestPath : manifestPaths) {
            log.debug("Reading manifest at path '{}'...", manifestPath);

            // Read the generated SBOM JSON file
            Bom bom = SbomUtils.fromPath(manifestPath);

            // If we couldn't read it, this is a fatal failure for us
            if (bom == null) {
                throw new ApplicationException("Could not read the manifest at '{}'", manifestPath.toAbsolutePath());
            }

            boms.add(bom);
        }

        return boms;
    }
}
