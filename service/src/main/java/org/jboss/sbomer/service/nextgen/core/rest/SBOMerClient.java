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
package org.jboss.sbomer.service.nextgen.core.rest;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.jboss.sbomer.service.nextgen.core.dto.model.EventRecord;
import org.jboss.sbomer.service.nextgen.core.dto.model.GenerationRecord;
import org.jboss.sbomer.service.nextgen.core.dto.model.ManifestRecord;
import org.jboss.sbomer.service.nextgen.core.payloads.generation.EventStatusUpdatePayload;
import org.jboss.sbomer.service.nextgen.core.payloads.generation.GenerationStatusUpdatePayload;
import org.jboss.sbomer.service.nextgen.core.payloads.generation.GenerationsRequest;
import org.jboss.sbomer.service.nextgen.core.payloads.generation.GenerationsResponse;
import org.jboss.sbomer.service.rest.otel.SpanName;
import org.jboss.sbomer.service.rest.otel.Traced;

import com.fasterxml.jackson.databind.JsonNode;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * A client for SBOMer REST API.
 */
@ApplicationScoped
// @ClientHeaderParam(name = "User-Agent", value = "SBOMer")
@RegisterRestClient(configKey = "sbomer")
@Path("/api/v1beta2")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface SBOMerClient {

    @Traced
    @SpanName("sbomer.generations.post")
    @POST
    @Path("/generations")
    GenerationsResponse requestGenerations(GenerationsRequest generationRecord);

    @Traced
    @SpanName("sbomer.generations.get.id")
    @GET
    @Path("/generations/{generationId}")
    GenerationRecord getGeneration(@PathParam("generationId") String generationId);

    @Traced
    @SpanName("sbomer.generations.patch.status")
    @PATCH
    @Path("/generations/{generationId}/status")
    GenerationRecord updateGenerationStatus(
            @PathParam("generationId") String generationId,
            GenerationStatusUpdatePayload payload);

    @Traced
    @SpanName("sbomer.generations.patch.manifests")
    @POST
    @Path("/generations/{generationId}/manifests")
    ManifestRecord uploadManifest(@PathParam("generationId") String generationId, JsonNode manifest);

    @Traced
    @SpanName("sbomer.events.patch.status")
    @PATCH
    @Path("/events/{eventId}/status")
    EventRecord updateEventStatus(@PathParam("eventId") String eventId, EventStatusUpdatePayload payload);
}
