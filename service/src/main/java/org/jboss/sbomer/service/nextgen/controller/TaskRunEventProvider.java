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

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import io.fabric8.tekton.v1beta1.TaskRun;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@ApplicationScoped
@Slf4j
public class TaskRunEventProvider {
    public final static String GENERATION_ID_LABEL = "sbomer.jboss.org/generation-id";

    KubernetesClient kubernetesClient;
    TaskRunEventHandler taskRunEventHandler;

    @Inject
    public TaskRunEventProvider(KubernetesClient kubernetesClient, TaskRunEventHandler taskRunEventHandler) {
        this.kubernetesClient = kubernetesClient;
        this.taskRunEventHandler = taskRunEventHandler;
    }

    void init(@Observes StartupEvent ev) {
        log.info("Instantiating informer for TaskRun");

        SharedIndexInformer<TaskRun> taskRunInformer = kubernetesClient.resources(TaskRun.class)
                .withLabel(GENERATION_ID_LABEL)
                .inform(taskRunEventHandler, 60 * 1000L); // TODO: Configure it

        taskRunInformer.stopped().whenComplete((v, t) -> {
            if (t != null) {
                log.error("Exception occurred, caught: {}", t.getMessage());
            }
        });
    }

}
