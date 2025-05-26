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
import org.jboss.sbomer.core.features.sbom.utils.ObjectMapperProvider;
import org.jboss.sbomer.service.feature.sbom.model.RandomStringIdGenerator;
import org.jboss.sbomer.service.feature.sbom.model.v1beta2.Event;
import org.jboss.sbomer.service.feature.sbom.model.v1beta2.Generation;
import org.jboss.sbomer.service.feature.sbom.model.v1beta2.dto.GenerationRecord;
import org.jboss.sbomer.service.feature.sbom.model.v1beta2.dto.V1Beta2Mapper;
import org.jboss.sbomer.service.feature.sbom.model.v1beta2.enums.EventType;
import org.jboss.sbomer.service.feature.sbom.model.v1beta2.enums.GenerationStatus;
import org.jboss.sbomer.service.rest.api.v1beta2.payloads.generation.GenerationsRequest;
import org.jboss.sbomer.service.rest.api.v1beta2.payloads.generation.GenerationsResponse;
import org.jboss.sbomer.service.rest.api.v1beta2.payloads.generation.UpdatePayload;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;

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
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;

@Path("/api/v1beta2/generations")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "SBOM Generations", description = "Endpoints for managing and interacting with SBOM generations.")
@Slf4j
public class GenerationsV1Beta2 {

    @Inject
    V1Beta2Mapper mapper;

    @POST
    @Operation(summary = "Request SBOM generations")
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
    public Response requestGenerations(@NotNull @Valid GenerationsRequest payload) {
        JsonNode context = null;

        try {
            context = ObjectMapperProvider.json().readTree(ObjectMapperProvider.json().writeValueAsString(payload));
        } catch (JsonMappingException e) {
            log.warn("Failed to map originatorContext to JsonNode", e);
        } catch (JsonProcessingException e) {
            log.warn("Failed to process originatorContext JSON", e);
        }

        Event event = Event.builder()
                .withId(RandomStringIdGenerator.generate())
                .withCreated(Instant.now())
                .withMetadata(Map.of("source", EventType.REST.toName()))
                .withEvent(context)
                .build()
                .save();

        payload.requests().forEach(request -> {
            log.debug("Processing request: {}", request.target());
            if (request.config() != null && request.config().resources() != null) {
                if (request.config().resources().requests() != null) {
                    log.debug("Requested specific requests resources: {}", request.config().resources().requests());
                }

                if (request.config().resources().limits() != null) {
                    log.debug("Requested specific limit resources: {}", request.config().resources().limits());
                }
            }

            JsonNode config = null;

            try {
                config = ObjectMapperProvider.json()
                        .readTree(ObjectMapperProvider.json().writeValueAsString(request.config()));
            } catch (JsonMappingException e) {
                log.warn("Failed to map originatorContext to JsonNode", e);
            } catch (JsonProcessingException e) {
                log.warn("Failed to process originatorContext JSON", e);
            }

            Generation generation = Generation.builder()
                    .withId(RandomStringIdGenerator.generate())
                    .withIdentifier(request.target().identifier())
                    .withConfig(config) // TODO: validate this and create effective config!!!
                    .withType(request.target().type())
                    .build()
                    .save();

            event.getGenerations().add(generation);
        });

        return Response.accepted(
                new GenerationsResponse(mapper.toRecord(event), mapper.toGenerationRecords(event.getGenerations())))
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