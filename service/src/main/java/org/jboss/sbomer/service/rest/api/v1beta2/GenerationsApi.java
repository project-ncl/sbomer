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
package org.jboss.sbomer.service.rest.api.v1beta2;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.sbomer.core.errors.NotFoundException;
import org.jboss.sbomer.core.utils.ObjectMapperUtils;
import org.jboss.sbomer.service.events.EventCreatedEvent;
import org.jboss.sbomer.service.feature.sbom.model.v1beta2.Event;
import org.jboss.sbomer.service.feature.sbom.model.v1beta2.Generation;
import org.jboss.sbomer.service.feature.sbom.model.v1beta2.dto.EventRecord;
import org.jboss.sbomer.service.feature.sbom.model.v1beta2.dto.GenerationRecord;
import org.jboss.sbomer.service.feature.sbom.model.v1beta2.dto.V1Beta2Mapper;
import org.jboss.sbomer.service.feature.sbom.model.v1beta2.enums.EventStatus;
import org.jboss.sbomer.service.feature.sbom.model.v1beta2.enums.EventType;
import org.jboss.sbomer.service.feature.sbom.model.v1beta2.enums.GenerationStatus;
import org.jboss.sbomer.service.rest.api.v1beta2.payloads.generation.GenerationRequestSpec;
import org.jboss.sbomer.service.rest.api.v1beta2.payloads.generation.GenerationsRequest;
import org.jboss.sbomer.service.rest.api.v1beta2.payloads.generation.GenerationsResponse;
import org.jboss.sbomer.service.rest.api.v1beta2.payloads.generation.UpdatePayload;
import org.jboss.sbomer.service.v1beta2.generator.GeneratorConfigProvider;

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

    V1Beta2Mapper mapper;

    GeneratorConfigProvider generatorConfigProvider;

    @Inject
    public GenerationsApi(V1Beta2Mapper mapper, GeneratorConfigProvider generatorConfigProvider) {
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

        Event event = Event.builder()
                .withCreated(Instant.now())
                .withMetadata(
                        Map.of(
                                EventsApi.KEY_SOURCE,
                                String.format("%s:%s", EventType.REST.toName(), uriInfo.getPath())))
                .withRequest(ObjectMapperUtils.toJsonNode(payload))
                .build()
                .save();

        payload.requests().forEach(request -> {
            log.debug("Processing request: '{}'", request.target());

            // Crucial step. From a request, create an effective config which selects the appropriate generator and
            // prepares its config.
            GenerationRequestSpec effectiveRequest = generatorConfigProvider.buildEffectiveRequest(request);

            log.debug("Effective request: '{}'", effectiveRequest);

            Generation generation = Generation.builder()
                    .withRequest(ObjectMapperUtils.toJsonNode(effectiveRequest))
                    .build()
                    .save();

            generation.updateStatus(GenerationStatus.NEW, "Generation created");

            event.getGenerations().add(generation);

        });

        event.updateStatus(EventStatus.NEW, "Received via REST API");

        EventRecord eventRecord = mapper.toRecord(event);

        Arc.container().beanManager().getEvent().fire(new EventCreatedEvent(eventRecord));

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

    @PATCH
    @Path("/{generationId}")
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
            @NotNull @Valid UpdatePayload payload) {

        Generation generation = Generation.findById(generationId); // NOSONAR

        if (generation == null) {
            throw new NotFoundException("Generation request with id '{}' could not be found", generationId);
        }

        // TODO: handle other payload elements
        generation.setStatus(GenerationStatus.GENERATING);

        return Response.ok(mapper.toRecord(generation)).build();
    }

}
