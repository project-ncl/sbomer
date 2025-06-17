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

import java.util.concurrent.TimeUnit;

import org.jboss.sbomer.service.leader.LeaderManager;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import io.fabric8.tekton.v1beta1.TaskRun;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.scheduler.Scheduled;
import io.quarkus.scheduler.Scheduled.ConcurrentExecution;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

/**
 * Prepares an informer for Kubernetes cluster and watches all {@link TaskRun}s which are related to generations.
 */
@ApplicationScoped
@Slf4j
public class GenerationTaskRunEventProvider {
    public final static String GENERATION_ID_LABEL = "sbomer.jboss.org/generation-id";

    KubernetesClient kubernetesClient;
    GenerationTaskRunEventHandler taskRunEventHandler;
    LeaderManager leaderManager;

    SharedIndexInformer<TaskRun> taskRunInformer;

    @Inject
    public GenerationTaskRunEventProvider(
            KubernetesClient kubernetesClient,
            GenerationTaskRunEventHandler taskRunEventHandler,
            LeaderManager leaderManager) {
        this.kubernetesClient = kubernetesClient;
        this.taskRunEventHandler = taskRunEventHandler;
        this.leaderManager = leaderManager;
    }

    void init(@Observes StartupEvent ev) {
        ensureInformer();
    }

    @Scheduled(every = "20s", delay = 10, delayUnit = TimeUnit.SECONDS, concurrentExecution = ConcurrentExecution.SKIP)
    void ensureInformer() {
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

        log.info("Instantiating informer for TaskRun");

        taskRunInformer = kubernetesClient.resources(TaskRun.class)
                .withLabel(GENERATION_ID_LABEL)
                .withLimit(50l)
                .inform(taskRunEventHandler, 60 * 1000L); // TODO: Configure it

        taskRunInformer.stopped().whenComplete((v, t) -> {
            if (t != null) {
                log.error("Exception occurred, caught: {}", t.getMessage());
            }
        });

    }
}
