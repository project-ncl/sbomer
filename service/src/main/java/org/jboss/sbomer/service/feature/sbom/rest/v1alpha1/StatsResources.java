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
package org.jboss.sbomer.service.feature.sbom.rest.v1alpha1;

import java.lang.management.ManagementFactory;
import java.time.Duration;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import jakarta.annotation.security.PermitAll;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.sbomer.service.feature.sbom.config.features.UmbConfig;
import org.jboss.sbomer.service.feature.sbom.features.umb.consumer.AmqpMessageConsumer;
import org.jboss.sbomer.service.feature.sbom.features.umb.producer.AmqpMessageProducer;
import org.jboss.sbomer.service.feature.sbom.model.Stats;
import org.jboss.sbomer.service.feature.sbom.model.Stats.Consumer;
import org.jboss.sbomer.service.feature.sbom.model.Stats.GenerationRequestStats;
import org.jboss.sbomer.service.feature.sbom.model.Stats.Messaging;
import org.jboss.sbomer.service.feature.sbom.model.Stats.Producer;
import org.jboss.sbomer.service.feature.sbom.model.Stats.Resources;
import org.jboss.sbomer.service.feature.sbom.model.Stats.SbomStats;
import org.jboss.sbomer.service.feature.sbom.service.SbomService;

@Path("/api/v1alpha1/stats")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
@Tag(name = "v1alpha1", description = "v1alpha1 API endpoints")
@PermitAll
public class StatsResources {

    @Inject
    SbomService sbomService;

    @Inject
    AmqpMessageConsumer messageConsumer;

    @Inject
    AmqpMessageProducer messageProducer;

    @Inject
    UmbConfig umbConfig;

    @ConfigProperty(name = "quarkus.application.version", defaultValue = "dev")
    String version;

    @GET
    @Operation(summary = "Get service runtime information", description = "Service information and statistics.")
    @APIResponses({ @APIResponse(
            responseCode = "200",
            description = "Available runtime information.",
            content = @Content(mediaType = MediaType.APPLICATION_JSON)) })
    public Response stats() {
        long uptimeMillis = getUptimeMillis();

        Messaging messaging = null;

        if (!umbConfig.isEnabled()) {
            messaging = Messaging.builder()
                    .withConsumer(
                            Consumer.builder()
                                    .withProcessed(messageConsumer.getProcessedMessages())
                                    .withReceived(messageConsumer.getReceivedMessages())
                                    .build())
                    .withProducer(
                            Producer.builder()
                                    .withAcked(messageProducer.getAckedMessages())
                                    .withNacked(messageProducer.getNackedMessages())
                                    .build())
                    .build();
        }

        Stats stats = Stats.builder()
                .withVersion(version)
                .withUptime(toUptime(uptimeMillis))
                .withUptimeMillis(uptimeMillis)
                .withResources(resources())
                .withMessaging(messaging)
                .build();

        return Response.status(Status.OK).entity(stats).build();
    }

    private long getUptimeMillis() {
        return ManagementFactory.getRuntimeMXBean().getUptime();
    }

    private Resources resources() {
        return Resources.builder().withSboms(sbomStats()).withGenerationRequests(generationRequestStats()).build();
    }

    private SbomStats sbomStats() {
        return SbomStats.builder().withTotal(sbomService.countSboms()).build();
    }

    private GenerationRequestStats generationRequestStats() {
        return GenerationRequestStats.builder()
                .withTotal(sbomService.countSbomGenerationRequests())
                .withInProgress(sbomService.countInProgressSbomGenerationRequests())
                .build();
    }

    private String toUptime(long milliseconds) {
        return Duration.ofMillis(milliseconds)
                .toString()
                .substring(2)
                .replaceAll("(\\d[HMS])(?!$)", "$1 ")
                .toLowerCase();
    }
}
