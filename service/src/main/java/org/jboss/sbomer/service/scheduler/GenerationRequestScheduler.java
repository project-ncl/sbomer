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
package org.jboss.sbomer.service.scheduler;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.jboss.sbomer.service.feature.sbom.k8s.model.GenerationRequest;
import org.jboss.sbomer.service.feature.sbom.k8s.model.GenerationRequestBuilder;
import org.jboss.sbomer.service.feature.sbom.k8s.model.SbomGenerationStatus;
import org.jboss.sbomer.service.feature.sbom.model.SbomGenerationRequest;
import org.jboss.sbomer.service.feature.sbom.service.SbomGenerationRequestRepository;
import org.jboss.sbomer.service.leader.LeaderManager;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.quarkus.scheduler.Scheduled;
import io.quarkus.scheduler.Scheduled.ConcurrentExecution;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.LockModeType;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;
import lombok.extern.slf4j.Slf4j;

@ApplicationScoped
@Slf4j
public class GenerationRequestScheduler {

    @Inject
    SbomGenerationRequestRepository requestRepository;

    @Inject
    KubernetesClient kubernetesClient;

    @Inject
    GenerationSchedulerConfig generationSchedulerConfig;

    @Inject
    LeaderManager leaderManager;

    /**
     * <p>
     * Schedules a batch of generations to be handled in the current deployment (if there is capacity available).
     * </p>
     *
     * <p>
     * If there are less than {@link GenerationSchedulerConfig#maxConcurrentGenerations()} generations currently running
     * it will check whether there are some generations waiting to be scheduled in the database. If there are such
     * generations found, it will fetch up to {@link GenerationSchedulerConfig#syncBatch()} generations and create
     * {@link GenerationRequest} instances (basically ConfigMap) for each one of them. These ill then will be picked by
     * the controller.
     * </p>
     *
     *
     * <p>
     * This method is run periodically. By default every 15 seconds. It is controlled by the
     * {@code sbomer.service.generation-scheduler.sync-interval} property.
     * </p>
     */
    @Scheduled(
            every = "${sbomer.service.generation-scheduler.sync-interval:15s}",
            delay = 1,
            delayUnit = TimeUnit.MINUTES,
            concurrentExecution = ConcurrentExecution.SKIP)
    @Transactional(value = TxType.REQUIRES_NEW)
    void scheduleGenerations() {
        if (!leaderManager.isLeader()) {
            log.info("Current instance is not the leader, skipping scheduling of generations in this instance");
            return;
        }

        // Get all ConfigMaps that represent generation requests within the namespace that are in progress
        int scheduledGenerationsCount = kubernetesClient.configMaps()
                .withLabelSelector(
                        "sbomer.jboss.org/type=generation-request,sbomer.jboss.org/status notin (FAILED, FINISHED)")
                .list()
                .getItems()
                .size();

        log.info("There are {} generations in progress", scheduledGenerationsCount);

        // In case we will exceed the max number of concurrent generations, do nothing and wait
        if (scheduledGenerationsCount > generationSchedulerConfig.maxConcurrentGenerations()) {
            log.info(
                    "The number of generations in progress: {} is higher than allowed to: {}, no new generations will be scheduled for now",
                    scheduledGenerationsCount,
                    generationSchedulerConfig.maxConcurrentGenerations());
            return;
        }

        log.debug("There is space in the cluster to process new generations, fetching them now...");

        List<SbomGenerationRequest> oldestResultsBatch = requestRepository
                .find("status = ?1 ORDER BY creationTime ASC", SbomGenerationStatus.NEW)
                .withLock(LockModeType.PESSIMISTIC_WRITE)
                .page(0, generationSchedulerConfig.syncBatch())
                .list();

        log.debug("Got {} generations to be scheduled...", oldestResultsBatch.size());

        oldestResultsBatch.forEach(g -> {
            g.setStatus(SbomGenerationStatus.SCHEDULED);
            schedule(g);
        });
    }

    /**
     * <p>
     * Syncs given {@link SbomGenerationRequest} with the cluster as {@link GenerationRequest}, if necessary.
     * </p>
     *
     * <p>
     * A new ConfigMap is created if it does not exist already.
     * </p>
     *
     * @param sbomGenerationRequest
     */
    public void schedule(SbomGenerationRequest sbomGenerationRequest) {
        log.debug("Scheduling Generation Request '{}'...", sbomGenerationRequest.getId());

        GenerationRequest request = (GenerationRequest) kubernetesClient.configMaps()
                .withName("sbom-request-" + sbomGenerationRequest.getId().toLowerCase())
                .get();

        if (request != null) {
            log.warn(
                    "The generation request '{}' already exists as a ConfigMap, skipping sync",
                    sbomGenerationRequest.getId());
            return;
        }

        request = new GenerationRequestBuilder(sbomGenerationRequest.getType()).withId(sbomGenerationRequest.getId())
                .withConfig(sbomGenerationRequest.getConfig())
                .withIdentifier(sbomGenerationRequest.getIdentifier())
                .withStatus(sbomGenerationRequest.getStatus())
                .withReason(sbomGenerationRequest.getReason())
                .withResult(sbomGenerationRequest.getResult())
                .build();

        ConfigMap cm = kubernetesClient.configMaps().resource(request).create();

        log.debug(
                "ConfigMap '{}' created as a representation of the Generation Request '{}'...",
                cm.getMetadata().getName(),
                sbomGenerationRequest.getId());

    }
}
