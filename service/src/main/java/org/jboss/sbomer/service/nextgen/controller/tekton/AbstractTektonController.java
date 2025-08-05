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
package org.jboss.sbomer.service.nextgen.controller.tekton;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import org.eclipse.microprofile.context.ManagedExecutor;
import org.jboss.sbomer.core.errors.ApplicationException;
import org.jboss.sbomer.core.features.sbom.utils.FileUtils;
import org.jboss.sbomer.service.feature.sbom.config.GenerationRequestControllerConfig;
import org.jboss.sbomer.service.feature.sbom.k8s.reconciler.TektonExitCodeUtils;
import org.jboss.sbomer.service.leader.LeaderManager;
import org.jboss.sbomer.service.nextgen.core.dto.model.GenerationRecord;
import org.jboss.sbomer.service.nextgen.core.dto.model.ManifestRecord;
import org.jboss.sbomer.service.nextgen.core.enums.GenerationResult;
import org.jboss.sbomer.service.nextgen.core.enums.GenerationStatus;
import org.jboss.sbomer.service.nextgen.core.generator.AbstractGenerator;
import org.jboss.sbomer.service.nextgen.core.rest.SBOMerClient;
import org.jboss.sbomer.service.nextgen.core.utils.ConfigUtils;
import org.jboss.sbomer.service.nextgen.core.utils.JacksonUtils;
import org.jboss.sbomer.service.nextgen.service.EntityMapper;

import com.fasterxml.jackson.databind.JsonNode;

import io.fabric8.knative.pkg.apis.Condition;
import io.fabric8.kubernetes.api.model.ContainerStateTerminated;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.informers.ResourceEventHandler;
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import io.fabric8.tekton.v1beta1.StepState;
import io.fabric8.tekton.v1beta1.TaskRun;
import io.fabric8.tekton.v1beta1.TaskRunStatus;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.validation.ValidationException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractTektonController extends AbstractGenerator
        implements ResourceEventHandler<TaskRun>, TektonController<GenerationRecord> {

    public static final String IS_OOM_KILLED = "OOMKilled";
    public final static String GENERATION_ID_LABEL = "sbomer.jboss.org/generation-id";
    public final static String GENERATOR_TYPE = "sbomer.jboss.org/generator-type";

    protected KubernetesClient kubernetesClient;

    protected GenerationRequestControllerConfig controllerConfig;

    protected String release;

    EntityMapper mapper;

    LeaderManager leaderManager;

    SharedIndexInformer<TaskRun> taskRunInformer;

    public AbstractTektonController(
            SBOMerClient sbomerClient,
            KubernetesClient kubernetesClient,
            GenerationRequestControllerConfig controllerConfig,
            ManagedExecutor managedExecutor,
            EntityMapper mapper,
            LeaderManager leaderManager) {
        super(sbomerClient, managedExecutor);

        this.kubernetesClient = kubernetesClient;
        this.release = ConfigUtils.getRelease();
        this.controllerConfig = controllerConfig;
        this.mapper = mapper;
        this.leaderManager = leaderManager;
    }

    /**
     * <p>
     * Ensure that we run the informer in case we are the leader. If we are not, stop any informer.
     * </p>
     * <p>
     * To properly function it is required that this method is run periodically.
     * </p>
     */
    protected void ensureInformer() {
        if (!leaderManager.isLeader()) {
            log.info("Current instance is not the leader, skipping instantiating TaskRun informer for this instance");

            if (taskRunInformer != null) {
                log.info("Cleaning up resources related to the informer");
                taskRunInformer.stop();
                taskRunInformer.close();
                taskRunInformer = null;
            }

            return;
        }

        if (taskRunInformer != null && taskRunInformer.isRunning()) {
            log.debug("Reusing current TaskRun informer");
            return;
        }

        log.info("Instantiating informer for TaskRun");

        taskRunInformer = kubernetesClient.resources(TaskRun.class)
                .withLabel(GENERATION_ID_LABEL)
                .withLabel(GENERATOR_TYPE, getGeneratorName())
                .inform(this, 60 * 1000L); // TODO: Configure it

        taskRunInformer.stopped().whenComplete((v, t) -> {
            if (t != null) {
                log.error("Exception occurred, caught: {}", t.getMessage());
            }
        });
    }

    @Override
    public void onAdd(TaskRun taskRun) {
        log.debug("{} TaskRun added", taskRun.getMetadata().getName());
        handle(taskRun);
    }

    @Override
    public void onUpdate(TaskRun oldTaskRun, TaskRun newTaskRun) {
        log.debug("{} TaskRun updated", newTaskRun.getMetadata().getName());
        handle(newTaskRun);
    }

    @Override
    public void onDelete(TaskRun taskRun, boolean deletedFinalStateUnknown) {
        log.info("{} TaskRun deleted", taskRun.getMetadata().getName());
        // TODO: setting generation status? Potentially to FAILED state.
    }

    /**
     * <p>
     * Read the Generation identifier from the TaskRun label {@link AbstractTektonController#GENERATION_ID_LABEL}.
     * </p>
     *
     * <p>
     * It will return {@code null} in case the label cannot be found.
     * </p>
     *
     * @param taskRun
     * @return The Generation identifier.
     */
    private String obtainGenerationId(TaskRun taskRun) {
        if (taskRun.getMetadata() == null || taskRun.getMetadata().getLabels() == null) {
            log.info(
                    "Task run '{}' does not have required '{}' annotations, skipping",
                    taskRun.getMetadata().getName(),
                    AbstractTektonController.GENERATION_ID_LABEL);
            return null;
        }

        return taskRun.getMetadata().getLabels().get(AbstractTektonController.GENERATION_ID_LABEL);
    }

    @ActivateRequestContext
    public void handle(TaskRun taskRun) {
        log.info("Handling TaskRun '{}'", taskRun.getMetadata().getName());

        // Get Generation identifier from the TaskRun label
        String generationId = obtainGenerationId(taskRun);

        // This TaskRun is not related to any Generation
        if (generationId == null) {
            log.warn(
                    "TaskRun '{}' is not related to any generation, it does not have required label: '{}', skipping",
                    taskRun.getMetadata().getName(),
                    AbstractTektonController.GENERATION_ID_LABEL);
            return;
        }

        GenerationRecord generationRecord = null;

        // Fetch Generation from the API
        try {
            generationRecord = sbomerClient.getGeneration(generationId);
        } catch (Exception e) {
            log.warn("Unable to fetch Generation with ID '{}', skipping", generationId, e);

            return;
        }

        log.debug("Finding TaskRuns related to Generation '{}'", generationId);

        // Find all TaskRuns that are related to this generation.
        List<TaskRun> relatedTaskRuns = kubernetesClient.resources(TaskRun.class)
                .withLabel(AbstractTektonController.GENERATION_ID_LABEL, generationId)
                .list()
                .getItems();

        // Reconcile!
        reconcile(generationRecord, new HashSet<>(relatedTaskRuns));
    }

    @Override
    public void reconcile(GenerationRecord generationRecord, Set<TaskRun> relatedTaskRuns) {
        log.info("Reconciling Generation {}", generationRecord);
        log.debug("Related TaskRuns: {}", relatedTaskRuns.stream().map(tr -> tr.getMetadata().getName()).toList());

        switch (generationRecord.status()) {
            case NEW:
                reconcileNew(generationRecord, relatedTaskRuns);
                break;
            case SCHEDULED:
                reconcileScheduled(generationRecord, relatedTaskRuns);
                break;
            case GENERATING:
                reconcileGenerating(generationRecord, relatedTaskRuns);
                break;
            case FINISHED:
                reconcileFinished(generationRecord, relatedTaskRuns);
                break;
            case FAILED:
                reconcileFailed(generationRecord, relatedTaskRuns);
                break;
            default:
                break;
        }
    }

    /**
     * Logic that should be performed when the status of the main resource is {@link GenerationStatus#GENERATING}.
     *
     * @param generation
     * @param relatedTaskRuns
     * @see GenerationStatus
     */
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

    protected void reconcileNew(GenerationRecord generation, Set<TaskRun> relatedTaskRuns) {
        log.debug("Reconcile '{}' for Generation '{}'...", GenerationStatus.NEW, generation.id());
        log.debug(
                "Ignoring, the {} ({}) controller does not act on this status",
                getGeneratorName(),
                getGeneratorVersion());
    }

    protected void reconcileScheduled(GenerationRecord generation, Set<TaskRun> relatedTaskRuns) {
        log.debug("Reconcile '{}' for Generation '{}'...", GenerationStatus.SCHEDULED, generation.id());
        log.debug(
                "Ignoring, the {} ({}) controller does not act on this status",
                getGeneratorName(),
                getGeneratorVersion());
    }

    protected void reconcileFinished(GenerationRecord generation, Set<TaskRun> relatedTaskRuns) {
        log.debug("Reconcile '{}' for Generation '{}'...", GenerationStatus.FINISHED, generation.id());
        cleanupFinishedGenerationRequest(generation, relatedTaskRuns);
    }

    protected void reconcileFailed(GenerationRecord generation, Set<TaskRun> relatedTaskRuns) {
        log.debug("Reconcile '{}' for Generation '{}'...", GenerationStatus.FAILED, generation.id());

        cleanupFinishedGenerationRequest(generation, relatedTaskRuns);
    }

    /**
     * Checks whether given {@link TaskRun} has finished or not.
     *
     * @param taskRun The {@link TaskRun} to check
     * @return {@code true} if the {@link TaskRun} finished, {@code false} otherwise
     */
    public boolean isFinished(TaskRun taskRun) {
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
    public Boolean isSuccessful(TaskRun taskRun) {
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

    public String getDetailedFailureMessage(TaskRun taskRun) {
        if (taskRun.getStatus() == null || taskRun.getStatus().getSteps() == null) {
            return "TaskRun failed with no step information available.";
        }

        return taskRun.getStatus().getSteps().stream().map(step -> {
            var term = step.getTerminated();
            if (term != null) {
                boolean isOomKilled = IS_OOM_KILLED.equals(term.getReason());
                boolean isFailedExit = term.getExitCode() != 0;

                if (isFailedExit || isOomKilled) {
                    String reason = TektonExitCodeUtils.interpretExitCode(term.getExitCode());
                    return String.format(
                            "Step '%s' failed: exitCode=%d (%s), reason=%s%s",
                            step.getName(),
                            term.getExitCode(),
                            reason,
                            term.getReason(),
                            term.getMessage() != null ? (", message=" + term.getMessage()) : "");
                }
            }

            return null;
        })
                .filter(Objects::nonNull)
                .findFirst()
                .orElse("TaskRun failed, but no step had non-zero exit code or OOMKilled reason.");
    }

    public boolean isOomKilled(TaskRun taskRun) {
        if (taskRun.getStatus() == null || taskRun.getStatus().getSteps() == null) {
            return false;
        }

        for (StepState step : taskRun.getStatus().getSteps()) {
            ContainerStateTerminated term = step.getTerminated();
            if (term != null && IS_OOM_KILLED.equals(term.getReason())) {
                return true;
            }
        }

        return false;
    }

    /**
     * Removes TaskRuns related to finished {@link GenerationRecord} as well ass all files in the shared volume.
     *
     * @param generation the generation request
     */
    protected void cleanupFinishedGenerationRequest(GenerationRecord generation, Set<TaskRun> relatedTaskRuns) {
        if (!controllerConfig.cleanup()) {
            log.debug(
                    "The cleanup setting is set to false, skipping cleaning up finished Generation '{}'",
                    generation.id());
            return;
        }

        Path workdirPath = Path.of(controllerConfig.sbomDir(), generation.id());

        log.debug(
                "Removing '{}' path being the working directory for the finished '{}' Generation",
                workdirPath.toAbsolutePath(),
                generation.id());

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

        relatedTaskRuns.forEach(tr -> {
            log.debug(
                    "Removing TaskRun '{}' as a result of cleaning up resources for Generation '{}'",
                    tr.getMetadata().getName(),
                    generation.id());
            kubernetesClient.resource(tr).delete();
        });
    }
}
