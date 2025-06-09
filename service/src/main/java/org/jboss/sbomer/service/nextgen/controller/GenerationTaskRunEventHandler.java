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

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.sbomer.service.nextgen.core.dto.EntityMapper;
import org.jboss.sbomer.service.nextgen.core.dto.GenerationRecord;
import org.jboss.sbomer.service.nextgen.core.rest.SBOMerClient;
import org.jboss.sbomer.service.nextgen.generator.syft.SyftController;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.informers.ResourceEventHandler;
import io.fabric8.tekton.v1beta1.TaskRun;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@ApplicationScoped
@Slf4j
public class GenerationTaskRunEventHandler implements ResourceEventHandler<TaskRun> {

    KubernetesClient kubernetesClient;
    SyftController generationController;
    EntityMapper mapper;
    SBOMerClient sbomerClient;

    @Inject
    public GenerationTaskRunEventHandler(
            KubernetesClient kubernetesClient,
            SyftController generationController,
            EntityMapper mapper,
            @RestClient SBOMerClient sbomerClient) {
        this.kubernetesClient = kubernetesClient;
        this.generationController = generationController;
        this.mapper = mapper;
        this.sbomerClient = sbomerClient;
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
                    TaskRunEventProvider.GENERATION_ID_LABEL);
            return;
        }

        GenerationRecord generationRecord = null;

        // Fetch Generation from the API
        try {
            generationRecord = sbomerClient.getGenerationById(generationId);
        } catch (Exception e) {
            log.warn("Unable to fetch Generation with ID '{}', skipping", generationId, e);
            return;
        }

        log.debug("Finding TaskRuns related to Generation '{}'", generationId);

        // Find all TaskRuns that are related to this generation.
        List<TaskRun> relatedTaskRuns = kubernetesClient.resources(TaskRun.class)
                .withLabel(TaskRunEventProvider.GENERATION_ID_LABEL, generationId)
                .list()
                .getItems();

        // Reconcile
        generationController.reconcile(generationRecord, new HashSet<>(relatedTaskRuns));
    }

    /**
     * Read the Generation identifier from the TaskRun label {@link TaskRunEventProvider.GENERATION_ID_LABEL}.
     * 
     * It will return {@code null} in case the label cannot be found.
     * 
     * @param taskRun
     * @return The Generation identifier.
     */
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
