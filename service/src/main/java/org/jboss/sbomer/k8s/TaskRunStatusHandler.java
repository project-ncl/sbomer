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
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.transaction.Transactional;

import org.jboss.sbomer.core.enums.SbomStatus;
import org.jboss.sbomer.core.utils.Constants;
import org.jboss.sbomer.model.Sbom;
import org.jboss.sbomer.service.SbomRepository;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.informers.ResourceEventHandler;
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import io.fabric8.kubernetes.client.informers.SharedInformerFactory;
import io.fabric8.tekton.pipeline.v1beta1.TaskRun;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.Startup;
import lombok.extern.slf4j.Slf4j;

/**
 * Handles updates of status for the {@link Sbom} resources based on the {@link TaskRun} updates.
 */
@ApplicationScoped
@Slf4j
public class TaskRunStatusHandler {

    @Inject
    KubernetesClient kubernetesClient;

    @Inject
    SbomRepository sbomRepository;

    SharedInformerFactory factory;

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
        factory = kubernetesClient.informers();

        SharedIndexInformer<TaskRun> informer = factory.sharedIndexInformerFor(TaskRun.class, resyncPeriod);

        informer.addEventHandler(new ResourceEventHandler<TaskRun>() {

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

        });

        factory.startAllRegisteredInformers();
    }

    /**
     * Check whether the {@link TaskRun} retrieved is valid for updating the {@link Sbom} resource status.
     *
     * @param taskRun The {@link TaskRun} instance
     * @return {@code true} if is valid, {@code false} otherwise
     */
    protected boolean isUpdateable(TaskRun taskRun) {
        String partOf = taskRun.getMetadata().getLabels().get(Constants.TEKTON_LABEL_NAME_APP_PART_OF);

        System.out.println(partOf);
        System.out.println(Constants.TEKTON_LABEL_VALUE_APP_PART_OF);

        // In case the TaskRun wasn't created by SBOMer, ignore it!
        if (!Objects.equals(Constants.TEKTON_LABEL_VALUE_APP_PART_OF, partOf)) {
            log.debug("Found Tekton TaskRun not related to SBOMer: '{}', skipping", taskRun.getMetadata().getName());
            return false;
        }

        String sbomId = taskRun.getMetadata().getLabels().get(Constants.TEKTON_LABEL_SBOM_ID);

        // In case the Sbom ID is not provided, it generally means a bug, but not a fatal one, hopefully
        if (sbomId == null) {
            log.warn(
                    "Found a Tekton TaskRun without the SBOM id label: '{}', skipping, but this is not good!",
                    taskRun.getMetadata().getName());
            return false;
        }

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
    protected void updateStatus(String sbomId, SbomStatus status) {

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
        // Update status
        statusCache.put(sbomId, status);
        // Save the resource
        sbomRepository.saveSbom(sbom);

        log.info("Updated Sbom id '{}' with status: '{}'", sbomId, status);

    }

    protected SbomStatus toStatus(TaskRun taskRun) {
        // Find last update
        String taskRunStatus = Optional.ofNullable(taskRun.getStatus().getConditions())
                .map(Collection::stream)
                .orElseGet(Stream::empty)
                .findFirst()
                .orElse(null)
                .getStatus();

        SbomStatus status = null;

        // Set the status based on the
        switch (taskRunStatus) {
            case "Unknown":
                status = SbomStatus.IN_PROGRESS;
                break;
            case "True":
                status = SbomStatus.READY;
                break;
            case "False":
                status = SbomStatus.FAILED;
                break;
            default:
                log.error("Received unknown status from TaskRun: '{}'", taskRunStatus);
                return null;
        }

        return status;
    }

    protected void handleTaskRunUpdate(TaskRun taskRun) {
        if (!isUpdateable(taskRun)) {
            return;
        }

        SbomStatus status = toStatus(taskRun);

        updateStatus(taskRun.getMetadata().getLabels().get(Constants.TEKTON_LABEL_SBOM_ID), status);
    }

    void onStop(@Observes ShutdownEvent ev) {
        factory.stopAllRegisteredInformers();
    }
}
