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
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.ExampleObject;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.common.util.RestMediaType;
import org.jboss.sbomer.core.errors.ErrorResponse;
import org.jboss.sbomer.core.errors.NotFoundException;
import org.jboss.sbomer.core.features.sbom.enums.GenerationRequestType;
import org.jboss.sbomer.core.utils.PaginationParameters;
import org.jboss.sbomer.service.feature.FeatureFlags;
import org.jboss.sbomer.service.feature.sbom.model.RandomStringIdGenerator;
import org.jboss.sbomer.service.feature.sbom.model.v1beta2.Event;
import org.jboss.sbomer.service.feature.sbom.model.v1beta2.Generation;
import org.jboss.sbomer.service.feature.sbom.model.v1beta2.dto.GenerationRecord;
import org.jboss.sbomer.service.feature.sbom.model.v1beta2.dto.V1Beta2Mapper;
import org.jboss.sbomer.service.feature.sbom.model.v1beta2.enums.EventStatus;
import org.jboss.sbomer.service.feature.sbom.model.v1beta2.enums.EventType;
import org.jboss.sbomer.service.feature.sbom.model.v1beta2.enums.GenerationStatus;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.jakarta.rs.yaml.YAMLMediaTypes;

import io.quarkus.resteasy.reactive.links.InjectRestLinks;
import io.quarkus.resteasy.reactive.links.RestLink;
import io.quarkus.resteasy.reactive.links.RestLinkType;
import jakarta.annotation.security.PermitAll;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;

@Path("/api/v1beta2/generations")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
@PermitAll
@Tag(name = "v1beta2")
@Slf4j
public class GenerationsV1Beta2 {
    @Inject
    V1Beta2Mapper mapper;

    @Inject
    FeatureFlags featureFlags;

    @Inject
    ObjectMapper objectMapper;

    @GET
    @Produces({ MediaType.APPLICATION_JSON, RestMediaType.APPLICATION_HAL_JSON })
    @RestLink(rel = "list")
    @InjectRestLinks
    @Operation(summary = "Search generations", description = "Paginated list of generations")
    @APIResponse(
            responseCode = "200",
            description = "Paginated list of generations found",
            content = @Content(mediaType = MediaType.APPLICATION_JSON))
    @APIResponse(
            responseCode = "500",
            description = "Internal server error",
            content = @Content(mediaType = MediaType.APPLICATION_JSON))
    public List<GenerationRecord> search( // TODO: USE paging
            @Valid @BeanParam PaginationParameters paginationParams,
            @QueryParam("query") String rsqlQuery,
            @DefaultValue("creationTime=desc=") @QueryParam("sort") String sort) {

        List<Generation> generations = Generation.findAll().list();

        System.out.println(mapper.toGenerationRecords(generations));

        try {
            System.out.println(objectMapper.writeValueAsString(mapper.toGenerationRecords(generations)));
        } catch (JsonProcessingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return mapper.toGenerationRecords(generations);

    }

    @GET
    @Path("/{id}")
    @Produces({ MediaType.APPLICATION_JSON, RestMediaType.APPLICATION_HAL_JSON })
    @RestLink(rel = "self")
    @InjectRestLinks(RestLinkType.INSTANCE)
    @Operation(
            summary = "Get specific manifest generation",
            description = "Get generation for the provided identifier or purl")
    @Parameter(
            name = "id",
            description = "Manifest generation identifier",
            examples = { @ExampleObject(value = "88CA2291D4014C6", name = "Generation identifier") })
    @APIResponse(
            responseCode = "200",
            description = "The generation",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = Map.class))) // TODO:
                                                                                                                      // populate
                                                                                                                      // it
    @APIResponse(
            responseCode = "400",
            description = "Malformed request",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = ErrorResponse.class)))
    @APIResponse(
            responseCode = "404",
            description = "Generation could not be found",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = ErrorResponse.class)))
    @APIResponse(
            responseCode = "500",
            description = "Internal server error",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = ErrorResponse.class)))
    public GenerationRecord getGenerationRequestById(@PathParam("id") String generationId) {
        Generation generation = Generation.findById(generationId); // NOSONAR

        if (generation == null) {
            throw new NotFoundException("Generation request with id '{}' could not be found", generationId);
        }

        return mapper.toRecord(generation);
    }

    @GET
    @Path("/{id}/events")
    @Operation(summary = "Receive events related to this generation")
    @Parameter(
            name = "id",
            description = "Manifest generation identifier",
            examples = { @ExampleObject(value = "88CA2291D4014C6", name = "Generation identifier") })
    @APIResponse(
            responseCode = "200",
            description = "Event list",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = Map.class))) // TODO:
                                                                                                                      // populate
                                                                                                                      // it
    @APIResponse(
            responseCode = "400",
            description = "Malformed request",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = ErrorResponse.class)))
    @APIResponse(
            responseCode = "404",
            description = "Generation could not be found",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = ErrorResponse.class)))
    @APIResponse(
            responseCode = "500",
            description = "Internal server error",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = ErrorResponse.class)))
    public Response getGenerationEvents(@PathParam("id") String generationId) {
        Generation generation = Generation.findById(generationId); // NOSONAR

        if (generation == null) {
            throw new NotFoundException("Generation request with id '{}' could not be found", generationId);
        }

        return Response.ok(mapper.toEventRecords(generation.getEvents())).build();
    }

    @POST
    @Transactional
    @Consumes({ MediaType.APPLICATION_JSON, YAMLMediaTypes.APPLICATION_JACKSON_YAML })
    @Operation(
            summary = "Request new generation",
            description = "Creates one or more generations within the system (depending on the request)")
    @RequestBody(
            content = @Content(
                    schema = @Schema(required = true, discriminatorProperty = "type", anyOf = {}), // TODO:
                                                                                                   // populate
                                                                                                   // it
                    examples = {})) // TODO: populate it
    @APIResponse(
            responseCode = "202",
            description = "Generation(s) successfully created",
            content = @Content(schema = @Schema(implementation = Map.class))) // TODO: populate it
    @APIResponse(
            responseCode = "400",
            description = "Failed while processing the request, please verify the provided content",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = ErrorResponse.class)))
    @APIResponse(
            responseCode = "500",
            description = "Internal server error",
            content = @Content(mediaType = MediaType.APPLICATION_JSON))
    public Response create(JsonNode request, @Context ContainerRequestContext requestContext) throws Exception {

        Generation generation = Generation.builder()
                .withId(RandomStringIdGenerator.generate())
                .withIdentifier("DUMMY")
                .withStatus(GenerationStatus.NEW)
                .withType(GenerationRequestType.BUILD.toName())
                .build();

        Event event = Event.builder()
                .withId(RandomStringIdGenerator.generate())
                .withCreated(Instant.now())
                .withStatus(EventStatus.NEW)
                .withSource(EventType.REST.toName()) // TODO: we don't have an enum here anymore
                .withEvent(JsonNodeFactory.instance.objectNode()) // TODO: dummy
                .withGenerations(List.of(generation))
                .build()
                .save();

        return Response.accepted(mapper.toRecord(event)).build();
    }

}
