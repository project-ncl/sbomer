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
package org.jboss.sbomer.service.nextgen.service;

import static org.jboss.sbomer.core.features.sbom.utils.MDCUtils.MDC_SPAN_ID_KEY;
import static org.jboss.sbomer.core.features.sbom.utils.MDCUtils.MDC_TRACE_FLAGS_KEY;
import static org.jboss.sbomer.core.features.sbom.utils.MDCUtils.MDC_TRACE_ID_KEY;
import static org.jboss.sbomer.core.features.sbom.utils.MDCUtils.MDC_TRACE_STATE_KEY;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.pnc.common.otel.OtelUtils;
import org.jboss.sbomer.service.feature.sbom.k8s.model.GenerationRequest;
import org.jboss.sbomer.service.feature.sbom.model.SbomGenerationRequest;
import org.jboss.sbomer.service.leader.LeaderManager;
import org.jboss.sbomer.service.nextgen.core.enums.GenerationStatus;
import org.jboss.sbomer.service.nextgen.core.events.GenerationStatusChangeEvent;
import org.jboss.sbomer.service.nextgen.core.utils.ConfigUtils;
import org.jboss.sbomer.service.nextgen.service.model.Generation;
import org.jboss.sbomer.service.scheduler.GenerationSchedulerConfig;
import org.slf4j.MDC;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Scope;
import io.quarkus.arc.Arc;
import io.quarkus.scheduler.Scheduled;
import io.quarkus.scheduler.Scheduled.ConcurrentExecution;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;
import lombok.extern.slf4j.Slf4j;

@ApplicationScoped
@Slf4j
public class GenerationEventSource {

    private final static String DEPLOYMENT_KEY = "deployment";

    KubernetesClient kubernetesClient;

    GenerationSchedulerConfig generationSchedulerConfig;

    LeaderManager leaderManager;

    EntityMapper mapper;

    String deploymentInfo;

    @Inject
    public GenerationEventSource(
            KubernetesClient kubernetesClient,
            GenerationSchedulerConfig generationSchedulerConfig,
            LeaderManager leaderManager,
            EntityMapper mapper) {
        this.kubernetesClient = kubernetesClient;
        this.generationSchedulerConfig = generationSchedulerConfig;
        this.leaderManager = leaderManager;
        this.mapper = mapper;

        String release = ConfigUtils.getRelease();
        String deploymentTarget = ConfigProvider.getConfig()
                .getOptionalValue("SBOMER_DEPLOYMENT_TARGET", String.class)
                .orElse("local");
        String deploymentType = ConfigProvider.getConfig()
                .getOptionalValue("SBOMER_DEPLOYMENT_TYPE", String.class)
                .orElse("dev");
        String deploymentZone = ConfigProvider.getConfig()
                .getOptionalValue("SBOMER_DEPLOYMENT_ZONE", String.class)
                .orElse("default");

        this.deploymentInfo = String.format("%s:%s:%s:%s", release, deploymentType, deploymentTarget, deploymentZone);

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
            delayUnit = TimeUnit.SECONDS,
            concurrentExecution = ConcurrentExecution.SKIP)
    public void scheduleGenerations() {
        if (!leaderManager.isLeader()) {
            log.info("Current instance is not the leader, skipping scheduling of generations in this instance");
            return;
        }

        long scheduledGenerationsCount = numberOfGenerationsInProgressInCluster();

        // In case we will exceed the max number of concurrent generations, do nothing and wait
        if (scheduledGenerationsCount > generationSchedulerConfig.maxConcurrentGenerations()) {
            log.info(
                    "The number of generations in progress: {} is higher than allowed to: {}, no new generations will be scheduled for now",
                    scheduledGenerationsCount,
                    generationSchedulerConfig.maxConcurrentGenerations());
            return;
        }

        fetchAndSchedule();
    }

    @Transactional(value = TxType.REQUIRES_NEW)
    protected void fetchAndSchedule() {
        log.debug("There is space in the cluster to process new generations, fetching them now...");

        @SuppressWarnings("unchecked")
        List<Generation> generations = Generation.getEntityManager()
                .createNativeQuery(
                        String.format(
                                "SELECT * FROM generation WHERE status = '%s' ORDER BY created ASC FOR UPDATE SKIP LOCKED LIMIT %s",
                                GenerationStatus.NEW,
                                generationSchedulerConfig.syncBatch()),
                        Generation.class)
                .getResultList();

        log.debug("Got {} generations to be scheduled...", generations.size());

        generations.forEach(g -> {
            g.setStatus(GenerationStatus.SCHEDULED);
            g.setReason("Scheduled for execution for {}", deploymentInfo);

            Map<String, String> metadata = new HashMap<>(Optional.ofNullable(g.getMetadata()).orElse(Map.of()));
            metadata.put(DEPLOYMENT_KEY, deploymentInfo);
            g.setMetadata(metadata);
            g.save();

            Arc.container().beanManager().getEvent().fire(new GenerationStatusChangeEvent(mapper.toRecord(g)));
        });
    }

    private long numberOfGenerationsInProgressInCluster() {
        log.debug("Counting generations running in the current deployment '{}'", deploymentInfo);

        long count = ((Number) Generation.getEntityManager()
                .createNativeQuery(
                        "SELECT count(*) FROM generation WHERE status = :status AND metadata ->> :deploymentKey = :deploymentInfo",
                        Long.class)
                .setParameter("status", GenerationStatus.GENERATING.name())
                .setParameter("deploymentKey", DEPLOYMENT_KEY)
                .setParameter("deploymentInfo", deploymentInfo)
                .getSingleResult()).longValue();

        log.debug("There are {} generations in progress", count);

        return count;
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
     * @param generation
     */
    public void schedule(Generation generation) {
        log.debug("Scheduling Generation Request '{}'...", generation.getId());

        // GenerationRequest request = (GenerationRequest) kubernetesClient.configMaps()
        // .withName("sbom-request-" + sbomGenerationRequest.getId().toLowerCase())
        // .get();

        // if (request != null) {
        // log.warn(
        // "The generation request '{}' already exists as a ConfigMap, skipping sync",
        // sbomGenerationRequest.getId());
        // return;
        // }

        // Create a parent child span with values from MDC. This is to differentiate each generationRequest with its own
        // span
        SpanBuilder spanBuilder = OtelUtils.buildChildSpan(
                GlobalOpenTelemetry.get().getTracer(""),
                "GenerationRequestScheduler.schedule",
                SpanKind.CLIENT,
                MDC.get(MDC_TRACE_ID_KEY),
                MDC.get(MDC_SPAN_ID_KEY),
                MDC.get(MDC_TRACE_FLAGS_KEY),
                MDC.get(MDC_TRACE_STATE_KEY),
                Span.current().getSpanContext(),
                Map.of());
        // Map.of(MDC_IDENTIFIER_KEY, generation.getIdentifier())); TODO: @avibelli

        Span span = spanBuilder.startSpan();

        log.debug(
                "Started a new span context with traceId: {}, spanId: {}, traceFlags: {}",
                span.getSpanContext().getTraceId(),
                span.getSpanContext().getSpanId(),
                span.getSpanContext().getTraceFlags().asHex());

        // put the span into the current Context
        try (Scope scope = span.makeCurrent()) {

            // generationController.reconcile(mapper.toRecord(generation), Collections.emptyList());
            // request = new GenerationRequestBuilder(GenerationRequestType.CONTAINERIMAGE)
            // .withId(sbomGenerationRequest.getId())
            // .withConfig(sbomGenerationRequest.getRequest().toString())
            // .withIdentifier(sbomGenerationRequest.getIdentifier())
            // .withStatus(sbomGenerationRequest.getStatus())
            // .withReason(sbomGenerationRequest.getReason())
            // .withResult(sbomGenerationRequest.getResult())
            // .withTraceId(span.getSpanContext().getTraceId())
            // .withSpanId(span.getSpanContext().getSpanId())
            // .withTraceParent(
            // OtelUtils.createTraceParent(
            // span.getSpanContext().getTraceId(),
            // span.getSpanContext().getSpanId(),
            // span.getSpanContext().getTraceFlags().asHex()))
            // .build();

            // ConfigMap cm = kubernetesClient.configMaps().resource(request).createOrReplace();

            // log.debug(
            // "ConfigMap '{}' created as a representation of the Generation Request '{}'...",
            // cm.getMetadata().getName(),
            // sbomGenerationRequest.getId());
        } finally {
            span.end(); // closing the scope does not end the span, this has to be done manually
        }
    }
}
