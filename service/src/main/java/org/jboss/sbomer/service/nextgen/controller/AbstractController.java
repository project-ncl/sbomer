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
package org.jboss.sbomer.service.nextgen.controller;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import org.cyclonedx.model.Bom;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.context.ManagedExecutor;
import org.jboss.sbomer.core.errors.ApplicationException;
import org.jboss.sbomer.core.errors.NotFoundException;
import org.jboss.sbomer.core.features.sbom.utils.SbomUtils;
import org.jboss.sbomer.service.feature.sbom.config.GenerationRequestControllerConfig;
import org.jboss.sbomer.service.feature.sbom.k8s.model.GenerationRequest;
import org.jboss.sbomer.service.feature.sbom.k8s.reconciler.TektonExitCodeUtils;
import org.jboss.sbomer.service.feature.sbom.model.v1beta2.Generation;
import org.jboss.sbomer.service.feature.sbom.model.v1beta2.dto.GenerationRecord;
import org.jboss.sbomer.service.feature.sbom.model.v1beta2.enums.GenerationResult;
import org.jboss.sbomer.service.feature.sbom.model.v1beta2.enums.GenerationStatus;
import org.slf4j.helpers.MessageFormatter;

import io.fabric8.knative.pkg.apis.Condition;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.tekton.v1beta1.TaskRun;
import io.fabric8.tekton.v1beta1.TaskRunStatus;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@NoArgsConstructor
public abstract class AbstractController implements Controller<GenerationRecord> {
    protected KubernetesClient kubernetesClient;

    protected GenerationRequestControllerConfig controllerConfig;

    protected String release;

    protected ManagedExecutor managedExecutor;

    @Inject
    public AbstractController(
            KubernetesClient kubernetesClient,
            GenerationRequestControllerConfig controllerConfig,
            ManagedExecutor managedExecutor) {
        this.kubernetesClient = kubernetesClient;
        this.release = ConfigProvider.getConfig().getOptionalValue("SBOMER_RELEASE", String.class).orElse("sbomer");
        this.controllerConfig = controllerConfig;
        this.managedExecutor = managedExecutor;
    }

    @Override
    public void reconcile(GenerationRecord generation, Set<TaskRun> relatedTaskRuns) {
        log.info("Reconciling {}", generation);
        log.info("Related TaskRuns: {}", relatedTaskRuns.stream().map(tr -> tr.getMetadata().getName()).toList());

        switch (generation.status()) {
            case NEW:
                reconcileNew(generation, relatedTaskRuns);
                break;
            case SCHEDULED:
                reconcileScheduled(generation, relatedTaskRuns);
                break;
            case GENERATING:
                reconcileGenerating(generation, relatedTaskRuns);
                break;
            case FINISHED:
                reconcileFinished(generation, relatedTaskRuns);
                break;
            case FAILED:
                reconcileFailed(generation, relatedTaskRuns);
                break;
            default:
                break;
        }
    }

    abstract protected void reconcileGenerating(GenerationRecord generation, Set<TaskRun> relatedTaskRuns);

    abstract protected TaskRun desired(GenerationRecord generation);

    void reconcileNew(GenerationRecord generation, Set<TaskRun> relatedTaskRuns) {
        log.debug("Reconcile '{}' for Generation '{}'...", GenerationStatus.NEW, generation.id());
        log.warn(
                "Got to reconcile a Generation with status '{}', ignoring, because we are expecting different statuses to act on",
                generation.status());
    }

    void reconcileScheduled(GenerationRecord generation, Set<TaskRun> relatedTaskRuns) {
        log.debug("Reconcile '{}' for Generation '{}'...", GenerationStatus.SCHEDULED, generation.id());

        updateStatus(generation, GenerationStatus.GENERATING, null, "Generation is in progress");

        try {
            kubernetesClient.resources(TaskRun.class).resource(desired(generation)).create();
        } catch (KubernetesClientException e) {
            updateStatus(
                    generation,
                    GenerationStatus.FAILED,
                    GenerationResult.ERR_SYSTEM,
                    "Unable to schedule Tekton TaskRun: " + e.getMessage());
        }
    }

    void reconcileFinished(GenerationRecord generation, Set<TaskRun> relatedTaskRuns) {
        log.debug("Reconcile '{}' for Generation '{}'...", GenerationStatus.FINISHED, generation.id());
        cleanupFinishedGenerationRequest(generation, relatedTaskRuns);
    }

    void reconcileFailed(GenerationRecord generation, Set<TaskRun> relatedTaskRuns) {
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
        })
                .filter(Objects::nonNull)
                .findFirst()
                .orElse("TaskRun failed, but no step had non-zero exit code or OOMKilled reason.");
    }

    /**
     * Reads manifests for given {@code manifestPaths} and converts them into {@link Bom}s.
     *
     * @param manifestPaths List of {@link Path}s to manifests in JSON format.
     * @return List of {@link Bom}s.
     */
    public List<Bom> readManifests(List<Path> manifestPaths) {
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

    /**
     * Removes related to finished {@link GenerationRequest} and its instance as well.
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

    @Transactional(value = Transactional.TxType.REQUIRES_NEW)
    protected void updateStatus(
            GenerationRecord generationRecord,
            GenerationStatus status,
            GenerationResult result,
            String reason,
            Object... params) {

        Generation generation = Generation.findById(generationRecord.id());

        if (generation == null) {
            throw new NotFoundException("Generation request with id '{}' could not be found", generationRecord.id());
        }

        String reasonContent = MessageFormatter.arrayFormat(reason, params).getMessage();

        generation.setStatus(status);
        generation.setReason(reasonContent);
        generation.setResult(result);
        generation.save();
    }
}
