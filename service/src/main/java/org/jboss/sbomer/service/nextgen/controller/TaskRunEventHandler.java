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

import java.util.HashSet;
import java.util.List;

import org.jboss.sbomer.service.feature.sbom.model.v1beta2.Generation;
import org.jboss.sbomer.service.feature.sbom.model.v1beta2.dto.V1Beta2Mapper;
import org.jboss.sbomer.service.nextgen.controller.syft.SyftController;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.informers.ResourceEventHandler;
import io.fabric8.tekton.v1beta1.TaskRun;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@ApplicationScoped
@Slf4j
public class TaskRunEventHandler implements ResourceEventHandler<TaskRun> {

    KubernetesClient kubernetesClient;
    SyftController generationController;
    V1Beta2Mapper mapper;

    @Inject
    public TaskRunEventHandler(
            KubernetesClient kubernetesClient,
            SyftController generationController,
            V1Beta2Mapper mapper) {
        this.kubernetesClient = kubernetesClient;
        this.generationController = generationController;
        this.mapper = mapper;
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
    }

    @ActivateRequestContext
    public void handle(TaskRun taskRun) {
        log.info("Handling TaskRun '{}'", taskRun.getMetadata().getName());

        String generationId = obtainGenerationId(taskRun);

        if (generationId == null) {
            log.warn(
                    "TaskRun '{}' does not have required label: '{}', skipping",
                    taskRun.getMetadata().getName(),
                    TaskRunEventProvider.GENERATION_ID_LABEL);
            return;
        }

        // Find the Generation
        Generation generation = Generation.findById(generationId);

        if (generation == null) {
            // TODO: delete TR?
            log.warn("Unable to find Generation with ID '{}', skipping", generationId);
            return;
        }

        log.debug("Finding TaskRuns related to Generation '{}'", generationId);

        List<TaskRun> relatedTaskRuns = kubernetesClient.resources(TaskRun.class)
                .withLabel(TaskRunEventProvider.GENERATION_ID_LABEL, generationId)
                .list()
                .getItems();

        // Reconcile
        generationController.reconcile(mapper.toRecord(generation), new HashSet<>(relatedTaskRuns));
    }

    private String obtainGenerationId(TaskRun taskRun) {
        if (taskRun.getMetadata() == null || taskRun.getMetadata().getLabels() == null) {
            log.info(
                    "Task run '{}' does not have required '{}' annotations, skipping",
                    taskRun.getMetadata().getName(),
                    TaskRunEventProvider.GENERATION_ID_LABEL);
            return null;
        }

        return taskRun.getMetadata().getLabels().get(TaskRunEventProvider.GENERATION_ID_LABEL);
    }
}
