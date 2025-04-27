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
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.jboss.pnc.api.constants.MDCKeys;
import org.jboss.pnc.common.otel.OtelUtils;
import org.jboss.sbomer.service.feature.sbom.k8s.model.GenerationRequest;
import org.jboss.sbomer.service.feature.sbom.k8s.model.GenerationRequestBuilder;
import org.jboss.sbomer.service.feature.sbom.k8s.model.SbomGenerationStatus;
import org.jboss.sbomer.service.feature.sbom.model.SbomGenerationRequest;
import org.jboss.sbomer.service.feature.sbom.service.SbomGenerationRequestRepository;
import org.jboss.sbomer.service.leader.LeaderManager;
import org.slf4j.MDC;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Scope;
import io.quarkus.scheduler.Scheduled;
import io.quarkus.scheduler.Scheduled.ConcurrentExecution;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;
import lombok.extern.slf4j.Slf4j;

@ApplicationScoped
@Slf4j
public class GenerationRequestScheduler {

    SbomGenerationRequestRepository requestRepository;

    KubernetesClient kubernetesClient;

    GenerationSchedulerConfig generationSchedulerConfig;

    LeaderManager leaderManager;

    @Inject
    public GenerationRequestScheduler(
            SbomGenerationRequestRepository requestRepository,
            KubernetesClient kubernetesClient,
            GenerationSchedulerConfig generationSchedulerConfig,
            LeaderManager leaderManager) {
        this.requestRepository = requestRepository;
        this.kubernetesClient = kubernetesClient;
        this.generationSchedulerConfig = generationSchedulerConfig;
        this.leaderManager = leaderManager;
    }

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
    public void scheduleGenerations() {
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

        List<SbomGenerationRequest> oldestResultsBatch = requestRepository.getEntityManager()
                .createNativeQuery(
                        String.format(
                                "SELECT * FROM sbom_generation_request WHERE status = '%s' ORDER BY creation_time ASC FOR UPDATE SKIP LOCKED LIMIT %s",
                                SbomGenerationStatus.NEW,
                                generationSchedulerConfig.syncBatch()),
                        SbomGenerationRequest.class)
                .getResultList();

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

        // Create a parent child span with values from MDC. This is to differentiate each generationRequest with its own
        // span
        SpanBuilder spanBuilder = OtelUtils.buildChildSpan(
                GlobalOpenTelemetry.get().getTracer(""),
                "GenerationRequestScheduler.schedule",
                SpanKind.CLIENT,
                MDC.get(MDCKeys.TRACE_ID_KEY),
                MDC.get(MDCKeys.SPAN_ID_KEY),
                MDC.get(MDCKeys.TRACE_FLAGS_KEY),
                MDC.get(MDCKeys.TRACE_STATE_KEY),
                Span.current().getSpanContext(),
                Map.of(MDCKeys.BUILD_ID_KEY, sbomGenerationRequest.getIdentifier()));
        Span span = spanBuilder.startSpan();

        log.debug(
                "Started a new span context with traceId: {}, spanId: {}, traceFlags: {}",
                span.getSpanContext().getTraceId(),
                span.getSpanContext().getSpanId(),
                span.getSpanContext().getTraceFlags().asHex());

        // put the span into the current Context
        try (Scope scope = span.makeCurrent()) {
            request = new GenerationRequestBuilder(sbomGenerationRequest.getType())
                    .withId(sbomGenerationRequest.getId())
                    .withConfig(sbomGenerationRequest.getConfig())
                    .withIdentifier(sbomGenerationRequest.getIdentifier())
                    .withStatus(sbomGenerationRequest.getStatus())
                    .withReason(sbomGenerationRequest.getReason())
                    .withResult(sbomGenerationRequest.getResult())
                    .withTraceId(span.getSpanContext().getTraceId())
                    .withSpanId(span.getSpanContext().getSpanId())
                    .withTraceParent(
                            OtelUtils.createTraceParent(
                                    span.getSpanContext().getTraceId(),
                                    span.getSpanContext().getSpanId(),
                                    span.getSpanContext().getTraceFlags().asHex()))
                    .build();

            ConfigMap cm = kubernetesClient.configMaps().resource(request).create();

            log.debug(
                    "ConfigMap '{}' created as a representation of the Generation Request '{}'...",
                    cm.getMetadata().getName(),
                    sbomGenerationRequest.getId());
        } finally {
            span.end(); // closing the scope does not end the span, this has to be done manually
        }

    }
}
