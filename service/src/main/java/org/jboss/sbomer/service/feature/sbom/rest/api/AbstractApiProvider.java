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
package org.jboss.sbomer.service.feature.sbom.rest.api;

import static org.jboss.sbomer.service.feature.sbom.UserRoles.SYSTEM_USER;

import java.lang.management.ManagementFactory;
import java.time.Duration;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.jboss.sbomer.core.config.ConfigSchemaValidator;
import org.jboss.sbomer.core.errors.NotFoundException;
import org.jboss.sbomer.core.features.sbom.utils.MDCUtils;
import org.jboss.sbomer.core.features.sbom.utils.SbomUtils;
import org.jboss.sbomer.service.feature.sbom.config.features.UmbConfig;
import org.jboss.sbomer.service.feature.sbom.features.umb.consumer.AmqpMessageConsumer;
import org.jboss.sbomer.service.feature.sbom.features.umb.producer.AmqpMessageProducer;
import org.jboss.sbomer.service.feature.sbom.model.Sbom;
import org.jboss.sbomer.service.feature.sbom.model.SbomGenerationRequest;
import org.jboss.sbomer.service.feature.sbom.model.Stats;
import org.jboss.sbomer.service.feature.sbom.model.Stats.Consumer;
import org.jboss.sbomer.service.feature.sbom.model.Stats.GenerationRequestStats;
import org.jboss.sbomer.service.feature.sbom.model.Stats.Messaging;
import org.jboss.sbomer.service.feature.sbom.model.Stats.Producer;
import org.jboss.sbomer.service.feature.sbom.model.Stats.Resources;
import org.jboss.sbomer.service.feature.sbom.model.Stats.SbomStats;
import org.jboss.sbomer.service.feature.sbom.service.SbomService;

import com.fasterxml.jackson.databind.JsonNode;

import io.fabric8.kubernetes.client.KubernetesClient;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;

public abstract class AbstractApiProvider {

    @Inject
    protected KubernetesClient kubernetesClient;

    @Inject
    protected SbomService sbomService;

    @Inject
    protected ConfigSchemaValidator configSchemaValidator;

    @Inject
    AmqpMessageConsumer messageConsumer;

    @Inject
    AmqpMessageProducer messageProducer;

    @Inject
    UmbConfig umbConfig;

    @ConfigProperty(name = "quarkus.application.version", defaultValue = "dev")
    String version;

    protected Sbom doGetSbomByPurl(String purl) {
        Sbom sbom = sbomService.findByPurl(purl);

        if (sbom == null) {
            throw new NotFoundException("SBOM with purl = '" + purl + "' couldn't be found");
        }

        return sbom;
    }

    protected Sbom doGetSbomById(String sbomId) {
        Sbom sbom = sbomService.get(sbomId);

        if (sbom == null) {
            throw new NotFoundException("SBOM with id '{}' not found", sbomId);
        }

        return sbom;
    }

    protected JsonNode doGetBomById(String sbomId) {
        Sbom sbom = doGetSbomById(sbomId);
        return SbomUtils.toJsonNode(sbom.getCycloneDxBom());
    }

    protected JsonNode doGetBomByPurl(String purl) {
        Sbom sbom = doGetSbomByPurl(purl);
        return SbomUtils.toJsonNode(sbom.getCycloneDxBom());
    }

    protected SbomGenerationRequest doGetSbomGenerationRequestById(String generationRequestId) {
        SbomGenerationRequest generationRequest = SbomGenerationRequest.findById(generationRequestId);

        if (generationRequest == null) {
            throw new NotFoundException("Generation request with id '{}' could not be found", generationRequestId);
        }

        return generationRequest;
    }

    @Path("/stats")
    @GET
    @Operation(summary = "Get service runtime information", description = "Service information and statistics.")
    @APIResponses({ @APIResponse(responseCode = "200", description = "Available runtime information.") })
    public Stats getStats() {
        long uptimeMillis = getUptimeMillis();

        Messaging messaging = null;

        if (umbConfig.isEnabled()) {
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

        return stats;
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

    @DELETE
    @Path("/sboms/requests/{id}")
    @RolesAllowed(SYSTEM_USER)
    @Operation(
            summary = "Delete SBOM generation request specified by id",
            description = "Delete the specified SBOM generation request from the database")
    @Parameter(name = "id", description = "The SBOM request identifier")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "SBOM generation request was successfully deleted"),
            @APIResponse(responseCode = "404", description = "Specified SBOM generation request could not be found"),
            @APIResponse(responseCode = "500", description = "Internal server error") })
    public Response deleteGenerationRequest(@PathParam("id") final String id) {

        try {
            MDCUtils.addProcessContext(id);
            sbomService.deleteSbomRequest(id);

            return Response.ok().build();
        } finally {
            MDCUtils.removeProcessContext();
        }
    }
}
