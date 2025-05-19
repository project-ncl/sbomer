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
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.ExampleObject;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.sbomer.core.errors.ErrorResponse;
import org.jboss.sbomer.core.errors.NotFoundException;
import org.jboss.sbomer.core.features.sbom.enums.EventStatus;
import org.jboss.sbomer.core.features.sbom.enums.EventType;
import org.jboss.sbomer.core.utils.PaginationParameters;
import org.jboss.sbomer.service.feature.sbom.model.RandomStringIdGenerator;
import org.jboss.sbomer.service.feature.sbom.model.v1beta2.Event;
import org.jboss.sbomer.service.feature.sbom.model.v1beta2.dto.V1Beta2Mapper;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;

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
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;

@Path("/api/v1beta2/events")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
@PermitAll
@Tag(name = "v1beta2")
@Slf4j
public class EventsV1Beta2 {

    @Inject
    V1Beta2Mapper mapper;

    @GET
    @Operation(
            summary = "Search events",
            description = "Performs a query according to the search criteria and returns paginated list of events")
    @APIResponse(
            responseCode = "200",
            description = "Paginated list of events",
            content = @Content(mediaType = MediaType.APPLICATION_JSON))
    @APIResponse(
            responseCode = "500",
            description = "Internal server error",
            content = @Content(mediaType = MediaType.APPLICATION_JSON))
    public Response search( // TODO: USE pagination
            @Valid @BeanParam PaginationParameters paginationParams,
            @QueryParam("query") String query,
            @DefaultValue("creationTime=desc=") @QueryParam("sort") String sort) {

        List<Event> events = Event.findAll().list();

        return Response.ok(mapper.toEventRecords(events)).build();

    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Get specific event", description = "Get event by the identifier")
    @Parameter(
            name = "id",
            description = "Event identifier",
            examples = { @ExampleObject(value = "88CA2291D4014C6", name = "Event identifier") })
    @APIResponse(
            responseCode = "200",
            description = "Event content",
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
            description = "Event could not be found",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = ErrorResponse.class)))
    @APIResponse(
            responseCode = "500",
            description = "Internal server error",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = ErrorResponse.class)))
    public Response getById(@PathParam("id") String eventId) {
        Event event = Event.findById(eventId); // NOSONAR

        if (event == null) {
            throw new NotFoundException("Event with id '{}' could not be found", eventId);
        }

        return Response.ok(mapper.toRecord(event)).build();
    }

    @POST
    @Path("/{id}/retry")
    @Operation(summary = "Retry an event", description = "Retry generations assigned to a particular event")
    @Parameter(
            name = "id",
            description = "Event identifier",
            required = true,
            examples = { @ExampleObject(value = "88CA2291D4014C6", name = "Event identifier") })
    @Parameter(
            name = "force",
            description = "Whether the retry should be considered a regeneration request (when set to true) or it should just regenerate failed generations and reuse successfully finished ones (when set to false)")
    @APIResponse(
            responseCode = "200",
            description = "Event content",
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
            description = "Event could not be found",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = ErrorResponse.class)))
    @APIResponse(
            responseCode = "500",
            description = "Internal server error",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = ErrorResponse.class)))
    @Transactional
    public Response retry(@PathParam("id") String eventId, @PathParam("force") boolean force) {
        Event parentEvent = Event.findById(eventId); // NOSONAR

        if (parentEvent == null) {
            throw new NotFoundException("Event with id '{}' could not be found", eventId);
        }

        // System.out.println(parentEvent.getGenerations());

        Event event = Event.builder()
                .withId(RandomStringIdGenerator.generate())
                .withParent(parentEvent)
                .withCreated(Instant.now())
                .withStatus(EventStatus.NEW)
                .withSource(EventType.REST.toName()) // TODO: we don't have an enum here anymore
                .withEvent(JsonNodeFactory.instance.objectNode()) // TODO: dummy
                .withGenerations(Collections.unmodifiableList(parentEvent.getGenerations()))
                .build()
                .save();

        return Response.ok(mapper.toRecord(event)).build();
    }
}
