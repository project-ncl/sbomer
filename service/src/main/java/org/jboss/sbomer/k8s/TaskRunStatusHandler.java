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
package org.jboss.sbomer.k8s;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.transaction.Transactional;

import org.gradle.internal.impldep.com.google.common.base.Objects;
import org.jboss.pnc.common.Strings;
import org.jboss.sbomer.config.ProcessingConfig;
import org.jboss.sbomer.core.enums.SbomStatus;
import org.jboss.sbomer.core.utils.Constants;
import org.jboss.sbomer.core.utils.MDCUtils;
import org.jboss.sbomer.features.umb.TaskRunsConfig;
import org.jboss.sbomer.features.umb.producer.NotificationService;
import org.jboss.sbomer.model.Sbom;
import org.jboss.sbomer.service.ProcessingService;
import org.jboss.sbomer.service.SbomRepository;
import org.jboss.sbomer.service.SbomService;

import io.fabric8.knative.internal.pkg.apis.Condition;
import io.fabric8.kubernetes.client.informers.ResourceEventHandler;
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import io.fabric8.tekton.client.TektonClient;
import io.fabric8.tekton.pipeline.v1beta1.TaskRunBuilder;
import io.fabric8.tekton.pipeline.v1beta1.TaskRun;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.Startup;
import lombok.extern.slf4j.Slf4j;

/**
 * Handles updates of status for the {@link Sbom} resources based on the {@link TaskRun} updates.
 *
 * @author Marek Goldmann
 */
@ApplicationScoped
@Slf4j
public class TaskRunStatusHandler {

    @Inject
    TektonClient tektonClient;

    @Inject
    SbomService sbomService;

    @Inject
    SbomRepository sbomRepository;

    @Inject
    ProcessingConfig processingConfig;

    @Inject
    ProcessingService processingService;

    @Inject
    NotificationService notificationService;

    @Inject
    TaskRunsConfig config;

    SharedIndexInformer<TaskRun> taskRunInformer;

    /**
     * A very simple cache so that we don't read the database to get the status of a particular resource.
     *
     * The only location where status updates are made is this piece of code, so it's safe to do so.
     */
    protected Map<String, SbomStatus> statusCache = new ConcurrentHashMap<>(0);

    /**
     * A resync period sync. At this interval -- we will re-request information about all resources.
     */
    private final long resyncPeriod = 60 * 1000L;

    @Startup
    public void onStart() {
        log.info("Starting handler for TaskRun status updates...");

        taskRunInformer = tektonClient.v1beta1()
                .taskRuns()
                .withLabel(Constants.TEKTON_LABEL_NAME_APP_PART_OF, Constants.TEKTON_LABEL_VALUE_APP_PART_OF)
                .withLabel(Constants.TEKTON_LABEL_SBOM_ID)
                .inform(new ResourceEventHandler<TaskRun>() {

                    @Override
                    public void onAdd(TaskRun taskRun) {
                        handleTaskRunUpdate(taskRun);
                    }

                    @Override
                    public void onUpdate(TaskRun oldTaskRun, TaskRun taskRun) {
                        handleTaskRunUpdate(taskRun);
                    }

                    @Override
                    public void onDelete(TaskRun taskRun, boolean deletedFinalStateUnknown) {

                    }

                }, resyncPeriod);

        taskRunInformer.start();

        log.info("Handler started!");
    }

    /**
     * Check whether the {@link TaskRun} retrieved is valid for updating the {@link Sbom} resource status.
     *
     * @param taskRun The {@link TaskRun} instance
     * @return {@code true} if is valid, {@code false} otherwise
     */
    protected boolean isUpdateable(TaskRun taskRun) {
        // TaskRun does not have proper status yet, nothing to update, skipping
        if (taskRun.getStatus() == null || taskRun.getStatus().getConditions() == null
                || taskRun.getStatus().getConditions().isEmpty()) {
            log.debug("Found Tekton TaskRun without status ready: '{}', skipping", taskRun.getMetadata().getName());
            return false;
        }

        log.debug("TaskRun '{}' is valid for processing", taskRun.getMetadata().getName());

        return true;
    }

    @Transactional
    protected void updateStatus(String sbomId, SbomStatus status, String taskRunFinalMsg) {

        SbomStatus cachedStatus = statusCache.get(sbomId);

        // If our cache has different content this means that we need to update the resource.
        // First update is always performed and the cache is populated with it.
        if (cachedStatus == status) {
            log.debug("Skipping update for Sbom id '{}' because new status is the same as old: '{}'", sbomId, status);
            return;
        }

        Sbom sbom = sbomRepository.findById(Long.valueOf(sbomId));

        if (sbom == null) {
            log.warn("Could not find Sbom id '{}', skipping updating the status", sbomId);
            return;
        }

        // Update resource
        sbom.setStatus(status);
        // Update the task run message
        sbom.setStatusMessage(taskRunFinalMsg);
        // Update status
        statusCache.put(sbomId, status);
        // Save the resource
        sbom = sbomRepository.saveSbom(sbom);

        log.info("Updated Sbom id '{}' with status: '{}'", sbomId, status);

        // If the taskRun has completed successfully, proceed with other tasks
        if (Objects.equal(status, SbomStatus.READY)) {

            if (sbom.isBase() && processingConfig.isEnabled() && processingConfig.shouldAutoProcess()) {
                // Trigger the process taskRun
                processingService.process(sbom);
            } else if (!sbom.isBase()) {
                // Notify that the enriched SBOMs is completed
                notificationService.notifyCompleted(String.valueOf(sbom.getId()));
            }
        }
    }

    protected SbomStatus toStatus(String taskRunStatus) {
        // Set the status based on the taskRunStatus.
        // See https://tekton.dev/docs/pipelines/taskruns/#monitoring-execution-status
        switch (taskRunStatus) {
            case "Unknown":
                return SbomStatus.IN_PROGRESS;
            case "True":
                return SbomStatus.READY;
            case "False":
                return SbomStatus.FAILED;
            default:
                log.error("Received unknown status from TaskRun: '{}'", taskRunStatus);
                return null;
        }
    }

    protected void handleTaskRunUpdate(TaskRun taskRun) {
        try {
            String buildId = taskRun.getMetadata().getLabels().get(Constants.TEKTON_LABEL_SBOM_BUILD_ID);
            String sbomId = taskRun.getMetadata().getLabels().get(Constants.TEKTON_LABEL_SBOM_ID);
            // make sure there is no build context
            MDCUtils.removeContext();
            MDCUtils.addBuildContext(buildId);
            MDCUtils.addProcessContext(String.valueOf(sbomId));

            if (!isUpdateable(taskRun)) {
                return;
            }

            // In case of unavailable conditions we don't do the update
            Optional<Condition> lastCondition = findLastCondition(taskRun);
            if (!lastCondition.isPresent()) {
                return;
            }

            // In case of an unknown status (it shouldn't happen!) we don't do the update
            SbomStatus status = toStatus(lastCondition.get().getStatus());
            if (status == null) {
                return;
            }

            // Get the final message in case the task run is completed
            String taskRunFinalMsg = getTaskRunFinalMessage(taskRun, lastCondition.get());

            // Update the Sbom status
            updateStatus(sbomId, status, taskRunFinalMsg);

            // Handle the taskRun if completed
            handleTaskRunCompleted(taskRun);
        } finally {
            MDCUtils.removeContext();
        }

    }

    protected void handleTaskRunCompleted(TaskRun taskRun) {

        if (taskRun.getStatus() != null && !Strings.isEmpty(taskRun.getStatus().getCompletionTime())) {

            Optional<Condition> lastCondition = findLastCondition(taskRun);
            if (lastCondition.isPresent()) {
                switch (lastCondition.get().getStatus()) {
                    case "True":
                        log.info("TaskRun '{}' completed successfully.", taskRun.getMetadata().getName());
                        if (config.cleanupSuccessful()) {
                            deleteTaskRun(taskRun);
                        }
                        break;
                    case "False":
                        log.info("TaskRun '{}' completed with failure.", taskRun.getMetadata().getName());
                        if (config.retries().isEnabled()) {
                            retryFailedTaskRun(taskRun, config.retries().maxRetries());
                        }

                        break;
                    default:
                        log.error(
                                "Task run '{}' is not completed, should not be here!",
                                taskRun.getMetadata().getName());
                        return;
                }
            }
        }
    }

    private void deleteTaskRun(TaskRun taskRun) {
        log.info("Deleting taskRun '{}'...", taskRun.getMetadata().getName());
        tektonClient.v1beta1().taskRuns().withName(taskRun.getMetadata().getName()).delete();
    }

    private void retryFailedTaskRun(TaskRun taskRun, int maxRetries) {

        int retryAttempt = getRetryAttempt(taskRun);
        if (retryAttempt < maxRetries) {

            log.info("Retrying failed taskRun '{}'...", taskRun.getMetadata().getName());

            // Metadata additional properties are not serialized/deserialized so they cannot be used
            // TaskRunSpec params are picked and interpreted as not valid by the CLI
            // So we are relying on the TaskRun name to set / get the retry number
            String newTaskRunName = generateNewTaskRunName(taskRun, retryAttempt + 1);
            TaskRun retryTaskRun = new TaskRunBuilder(taskRun).editMetadata()
                    .withName(newTaskRunName)
                    .endMetadata()
                    .build();

            deleteTaskRun(taskRun);

            log.info("Creating new taskRun '{}'...", retryTaskRun.getMetadata().getName());
            tektonClient.v1beta1().taskRuns().resource(retryTaskRun).createOrReplace();
        } else {
            log.info(
                    "Reached the maximum number of retries ({}) for failed taskRun '{}', giving up",
                    maxRetries,
                    taskRun.getMetadata().getName());
        }
    }

    protected Optional<Condition> findLastCondition(TaskRun taskRun) {
        return Optional.ofNullable(taskRun.getStatus().getConditions())
                .map(Collection::stream)
                .orElseGet(Stream::empty)
                .findFirst();
    }

    private String getTaskRunFinalMessage(TaskRun taskRun, Condition condition) {

        if (Strings.isEmpty(taskRun.getStatus().getCompletionTime())) {
            return null;
        }

        String msg = condition.getReason();
        if (!Strings.isEmpty(condition.getMessage())) {
            msg += " - " + condition.getMessage();
        }
        return msg;
    }

    private int getRetryAttempt(TaskRun taskRun) {
        if (taskRun.getMetadata()
                .getName()
                .lastIndexOf("-" + Constants.TEKTON_TASK_RUN_NAME_SUFFIX_RETRY_ATTEMPT + "-") == -1) {
            return 0;
        }

        int index = taskRun.getMetadata().getName().lastIndexOf("-") + 1;
        try {
            return Integer.valueOf(taskRun.getMetadata().getName().substring(index));
        } catch (NumberFormatException nfe) {
            log.warn(
                    "Property '{}' not set correctly in taskRun name, will restart all retries.",
                    Constants.TEKTON_TASK_RUN_NAME_SUFFIX_RETRY_ATTEMPT);
        }
        return 0;
    }

    private String generateNewTaskRunName(TaskRun taskRun, int retryAttempt) {
        String newTaskRunName = taskRun.getMetadata().getName();
        if (newTaskRunName.lastIndexOf("-" + Constants.TEKTON_TASK_RUN_NAME_SUFFIX_RETRY_ATTEMPT + "-") == -1) {
            newTaskRunName += ("-" + Constants.TEKTON_TASK_RUN_NAME_SUFFIX_RETRY_ATTEMPT + "-" + retryAttempt);
        } else {
            int index = newTaskRunName.lastIndexOf("-") + 1;
            newTaskRunName = newTaskRunName.substring(0, index) + retryAttempt;
        }
        return newTaskRunName;
    }

    void onStop(@Observes ShutdownEvent ev) {
        log.info("Stopping handler for TaskRun status updates...");

        if (taskRunInformer != null) {
            taskRunInformer.stop();
        }

        log.info("Handler stopped!");
    }
}
