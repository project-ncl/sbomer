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

import static org.jboss.sbomer.service.feature.sbom.UserRoles.USER_DELETE_ROLE;

import java.lang.management.ManagementFactory;
import java.time.Duration;

import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.jboss.sbomer.core.config.ConfigSchemaValidator;
import org.jboss.sbomer.core.errors.NotFoundException;
import org.jboss.sbomer.core.features.sbom.utils.MDCUtils;
import org.jboss.sbomer.core.features.sbom.utils.SbomUtils;
import org.jboss.sbomer.service.feature.FeatureFlags;
import org.jboss.sbomer.service.feature.sbom.config.features.UmbConfig;
import org.jboss.sbomer.service.feature.sbom.features.umb.consumer.AmqpMessageConsumer;
import org.jboss.sbomer.service.feature.sbom.features.umb.producer.AmqpMessageProducer;
import org.jboss.sbomer.service.feature.sbom.model.Sbom;
import org.jboss.sbomer.service.feature.sbom.model.SbomGenerationRequest;
import org.jboss.sbomer.service.feature.sbom.model.Stats;
import org.jboss.sbomer.service.feature.sbom.model.Stats.Consumer;
import org.jboss.sbomer.service.feature.sbom.model.Stats.Deployment;
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
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import lombok.extern.slf4j.Slf4j;

@Slf4j
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
    FeatureFlags featureFlags;

    @Inject
    UmbConfig umbConfig;

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
        SbomGenerationRequest generationRequest = SbomGenerationRequest.findById(generationRequestId); // NOSONAR

        if (generationRequest == null) {
            throw new NotFoundException("Generation request with id '{}' could not be found", generationRequestId);
        }

        return generationRequest;
    }

    @Path("/stats")
    @GET
    @Operation(summary = "Get service runtime information", description = "Service information and statistics.")
    @APIResponse(responseCode = "200", description = "Available runtime information.")
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
                .withVersion(
                        ConfigProvider.getConfig()
                                .getOptionalValue("quarkus.application.version", String.class)
                                .orElse("dev"))
                .withUptime(toUptime(uptimeMillis))
                .withUptimeMillis(uptimeMillis)
                .withResources(resources())
                .withMessaging(messaging)
                .withRelease(ConfigProvider.getConfig().getOptionalValue("sbomer.release", String.class).orElse("dev"))
                .withAppEnv(ConfigProvider.getConfig().getOptionalValue("app.env", String.class).orElse("dev"))
                .withHostname(ConfigProvider.getConfig().getOptionalValue("hostname", String.class).orElse(null))
                .withDeployment(
                        Deployment.builder()
                                .withTarget(
                                        ConfigProvider.getConfig()
                                                .getOptionalValue("sbomer.deployment.target", String.class)
                                                .orElse("dev"))
                                .withType(
                                        ConfigProvider.getConfig()
                                                .getOptionalValue("sbomer.deployment.type", String.class)
                                                .orElse("dev"))
                                .withZone(
                                        ConfigProvider.getConfig()
                                                .getOptionalValue("sbomer.deployment.zone", String.class)
                                                .orElse("dev"))
                                .build())
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
    @RolesAllowed(USER_DELETE_ROLE)
    @Operation(
            summary = "Delete SBOM generation request specified by id",
            description = "Delete the specified SBOM generation request from the database")
    @Parameter(name = "id", description = "The SBOM request identifier")
    @APIResponse(responseCode = "200", description = "SBOM generation request was successfully deleted")
    @APIResponse(responseCode = "404", description = "Specified SBOM generation request could not be found")
    @APIResponse(responseCode = "500", description = "Internal server error")
    public Response deleteGenerationRequest(@PathParam("id") final String id) {

        try {
            MDCUtils.addProcessContext(id);
            sbomService.deleteSbomRequest(id);

            return Response.ok().build();
        } finally {
            MDCUtils.removeProcessContext();
        }
    }

    @POST
    @Operation(
            summary = "Resend UMB notification message for a completed SBOM",
            description = "Force the resending of the UMB notification message for an already generated SBOM.")
    @Parameter(name = "id", description = "SBOM identifier", example = "429305915731435500")
    @Path("/sboms/{id}/notify")
    @APIResponse(responseCode = "200")
    @APIResponse(
            responseCode = "404",
            description = "Requested SBOM could not be found",
            content = @Content(mediaType = MediaType.APPLICATION_JSON))
    @APIResponse(
            responseCode = "500",
            description = "Internal server error",
            content = @Content(mediaType = MediaType.APPLICATION_JSON))
    public Response notify(@PathParam("id") String sbomId) throws Exception {
        if (featureFlags.isDryRun()) {
            log.warn("Skipping notification for SBOM '{}' because of SBOMer running in dry-run mode", sbomId);
            return Response.status(Status.SERVICE_UNAVAILABLE).build();
        }

        Sbom sbom = doGetSbomById(sbomId);
        sbomService.notifyCompleted(sbom);
        return Response.ok().build();
    }
}
