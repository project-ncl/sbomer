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
package org.jboss.sbomer.service.nextgen.service.rest.v1beta2;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.sbomer.core.errors.ClientException;
import org.jboss.sbomer.core.errors.NotFoundException;
import org.jboss.sbomer.service.nextgen.core.dto.model.EventRecord;
import org.jboss.sbomer.service.nextgen.core.dto.model.GenerationRecord;
import org.jboss.sbomer.service.nextgen.core.dto.model.GenerationStatusRecord;
import org.jboss.sbomer.service.nextgen.core.dto.model.ManifestRecord;
import org.jboss.sbomer.service.nextgen.core.enums.EventStatus;
import org.jboss.sbomer.service.nextgen.core.events.EventStatusChangeEvent;
import org.jboss.sbomer.service.nextgen.core.events.GenerationStatusChangeEvent;
import org.jboss.sbomer.service.nextgen.core.payloads.generation.GenerationStatusUpdatePayload;
import org.jboss.sbomer.service.nextgen.core.payloads.generation.GenerationsRequest;
import org.jboss.sbomer.service.nextgen.core.payloads.generation.GenerationsResponse;
import org.jboss.sbomer.service.nextgen.core.utils.JacksonUtils;
import org.jboss.sbomer.service.nextgen.service.EntityMapper;
import org.jboss.sbomer.service.nextgen.service.config.GeneratorConfigProvider;
import org.jboss.sbomer.service.nextgen.service.model.Event;
import org.jboss.sbomer.service.nextgen.service.model.Generation;
import org.jboss.sbomer.service.nextgen.service.model.Manifest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.quarkus.arc.Arc;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Path("/api/v1beta2/generations")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "v1beta2")
@Slf4j
@NoArgsConstructor
public class GenerationsApi {

    EntityMapper mapper;

    GeneratorConfigProvider generatorConfigProvider;

    @Inject
    public GenerationsApi(EntityMapper mapper, GeneratorConfigProvider generatorConfigProvider) {
        this.mapper = mapper;
        this.generatorConfigProvider = generatorConfigProvider;
    }

    @POST
    @Operation(summary = "Request manifest generations")
    @APIResponse(
            responseCode = "202",
            description = "Generations request accepted",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(type = SchemaType.ARRAY, implementation = GenerationsResponse.class)))
    @APIResponse(
            responseCode = "207",
            description = "Generations request partially processed (some items may have validation errors)",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(type = SchemaType.ARRAY, implementation = GenerationsResponse.class)))
    @APIResponse(responseCode = "400", description = "Invalid request payload")
    @Transactional
    public Response requestGenerations(@NotNull @Valid GenerationsRequest payload, @Context UriInfo uriInfo) {

        Event event;

        if (payload.eventId() != null) {
            event = Event.findById(payload.eventId());

            if (event == null) {
                throw new ClientException(
                        "Unable to find Event with id '{}', processing this request cannot continue. Make sure you either: provide correct event id or remove it entirely.",
                        payload.eventId());
            }

            // If the event exist, let's merge the original request with the current one which contains generations as
            // well.
            JsonNode mergedRequest = JacksonUtils.merge(event.getRequest(), JacksonUtils.toObjectNode(payload));

            event.setRequest(mergedRequest);

        } else {
            event = Event.builder()
                    .withCreated(Instant.now())
                    .withMetadata(
                            Map.of(EventsApi.KEY_SOURCE, String.format("%s:%s", Api.EVENT_TYPE, uriInfo.getPath())))
                    .withRequest(JacksonUtils.toObjectNode(payload))
                    .withReason("Created as a result of a REST API call")
                    .build()
                    .save();
        }

        event.setStatus(EventStatus.INITIALIZING);
        event.setReason("Event is being initialized");
        event.save();

        EventRecord eventRecord = mapper.toRecord(event);

        Arc.container().beanManager().getEvent().fire(new EventStatusChangeEvent(eventRecord));

        return Response
                .accepted(new GenerationsResponse(eventRecord, mapper.toGenerationRecords(event.getGenerations())))
                .build();
    }

    @GET
    @Operation(summary = "List SBOM generations")
    @APIResponse(
            responseCode = "200",
            description = "A list of generations",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(type = SchemaType.ARRAY, implementation = GenerationRecord.class)))
    public Response listGenerations(
            @QueryParam("status") String status,
            @QueryParam("targetIdentifier") String targetIdentifier,
            // Add other relevant filter parameters (e.g., targetType, eventId)
            @QueryParam("pageIndex") @DefaultValue("0") int pageIndex,
            @QueryParam("pageSize") @DefaultValue("10") int pageSize) {

        List<Generation> generations = Generation.findAll().list();

        return Response.ok(mapper.toGenerationRecords(generations)).header("X-Total-Count", generations.size()).build();
    }

    @GET
    @Path("/{generationId}")
    @Operation(summary = "Get details of a generation")
    @APIResponse(
            responseCode = "200",
            description = "Details of the generation",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = GenerationRecord.class)))
    @APIResponse(responseCode = "404", description = "Generation not found")
    public Response getGenerationById(@PathParam("generationId") String generationId) {
        Generation generation = Generation.findById(generationId); // NOSONAR

        if (generation == null) {
            throw new NotFoundException("Generation request with id '{}' could not be found", generationId);
        }

        return Response.ok(mapper.toRecord(generation)).build();
    }

    @GET
    @Path("/{generationId}/manifests")
    @Operation(summary = "Get all manifests related to a particular generation")
    @APIResponse(
            responseCode = "200",
            description = "List of manifests",
            content = @Content(mediaType = MediaType.APPLICATION_JSON))
    @APIResponse(responseCode = "404", description = "Generation not found")
    public List<ManifestRecord> getManifestsForGeneration(@PathParam("generationId") String generationId) {
        Generation generation = Generation.findById(generationId); // NOSONAR

        if (generation == null) {
            throw new NotFoundException("Generation request with id '{}' could not be found", generationId);
        }

        return mapper.toManifestRecords(generation.getManifests());
    }

    @PATCH
    @Path("/{generationId}/status")
    @Operation(summary = "Update progress of a generation task (Worker only)")
    @APIResponse(
            responseCode = "200",
            description = "Progress updated",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = GenerationRecord.class)))
    @APIResponse(responseCode = "404", description = "Generation not found")
    @Transactional
    public Response updateGenerationProgress(
            @PathParam("generationId") String generationId,
            @NotNull @Valid GenerationStatusUpdatePayload payload) {

        Generation generation = Generation.findById(generationId); // NOSONAR

        if (generation == null) {
            throw new NotFoundException("Generation request with id '{}' could not be found", generationId);
        }

        generation.setStatus(payload.status());
        generation.setResult(payload.result());
        generation.setReason(payload.reason());
        generation.save();

        GenerationRecord generationRecord = mapper.toRecord(generation);

        Arc.container().beanManager().getEvent().fire(new GenerationStatusChangeEvent(generationRecord));

        return Response.ok(generationRecord).build();
    }

    @GET
    @Path("/{generationId}/history")
    @Operation(summary = "Get status history of a generation")
    @APIResponse(
            responseCode = "200",
            description = "Status history",
            content = @Content(mediaType = MediaType.APPLICATION_JSON))
    @APIResponse(responseCode = "404", description = "Generation not found")
    public List<GenerationStatusRecord> getStatusesForGeneration(@PathParam("generationId") String generationId) {
        Generation generation = Generation.findById(generationId); // NOSONAR

        if (generation == null) {
            throw new NotFoundException("Generation request with id '{}' could not be found", generationId);
        }

        return mapper.toGenerationStatusRecords(generation.getStatuses());
    }

    @POST
    @Path("/{generationId}/manifests")
    @Operation(summary = "Upload new manifest and attach it to a generation (Worker only)")
    @APIResponse(
            responseCode = "200",
            description = "Manifest uploaded",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = ManifestRecord.class)))
    @APIResponse(responseCode = "404", description = "Generation not found")
    @Transactional
    public Response uploadManifest(@PathParam("generationId") String generationId, JsonNode payload) {

        log.info("About to store manifests for generation {}", generationId);

        Generation generation = Generation.findById(generationId); // NOSONAR

        if (generation == null) {
            throw new NotFoundException("Generation request with id '{}' could not be found", generationId);
        }

        log.debug("Preparing new manifest entity for the payload");

        ObjectNode metadata = JsonNodeFactory.instance.objectNode();
        metadata.put("sha256", JacksonUtils.hash(payload));

        Manifest manifest = Manifest.builder()
                .withGeneration(generation)
                .withBom(payload)
                .withMetadata(metadata)
                .build()
                .save();
        generation.getManifests().add(manifest);
        generation.save();

        return Response.ok(mapper.toRecord(manifest)).build();
    }

}
