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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.ExampleObject;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.sbomer.core.errors.ErrorResponse;
import org.jboss.sbomer.core.errors.NotFoundException;
import org.jboss.sbomer.core.features.sbom.rest.Page;
import org.jboss.sbomer.core.utils.PaginationParameters;
import org.jboss.sbomer.service.nextgen.antlr4.antlr.QueryLexer;
import org.jboss.sbomer.service.nextgen.antlr4.antlr.QueryParser;
import org.jboss.sbomer.service.nextgen.antlr4.antlr.QueryParser.QueryContext;
import org.jboss.sbomer.service.nextgen.core.dto.model.EventRecord;
import org.jboss.sbomer.service.nextgen.core.dto.model.EventStatusRecord;
import org.jboss.sbomer.service.nextgen.core.dto.model.GenerationRecord;
import org.jboss.sbomer.service.nextgen.core.enums.GenerationStatus;
import org.jboss.sbomer.service.nextgen.core.events.EventStatusChangeEvent;
import org.jboss.sbomer.service.nextgen.core.payloads.generation.EventStatusUpdatePayload;
import org.jboss.sbomer.service.nextgen.query.JpqlQueryListener;
import org.jboss.sbomer.service.nextgen.service.EntityMapper;
import org.jboss.sbomer.service.nextgen.service.model.Event;
import org.jboss.sbomer.service.nextgen.service.model.Generation;
import org.jboss.sbomer.service.nextgen.service.rest.RestUtils;

import io.quarkus.arc.Arc;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.vertx.core.eventbus.EventBus;
import jakarta.annotation.security.PermitAll;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
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

@Path("/api/v1beta2/events")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
@PermitAll
@Tag(name = "v1beta2")
@Slf4j
public class EventsApi {

    public static final String KEY_SOURCE = "source"; // TODO: externalize it
    public static final String KEY_RESOLVER = "resolver"; // TODO: externalize it
    public static final String KEY_IDENTIFIER = "identifier"; // TODO: externalize it

    @Inject
    EntityMapper mapper;

    @Inject
    EventBus eventBus;

    @GET
    @Operation(
            summary = "Search events",
            description = "Performs a query according to the search criteria and returns paginated list of events")
    @APIResponse(
            responseCode = "200",
            description = "Paginated list of events",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(type = SchemaType.OBJECT, implementation = Page.class)))
    @APIResponse(
            responseCode = "500",
            description = "Internal server error",
            content = @Content(mediaType = MediaType.APPLICATION_JSON))
    public Response search(@Valid @BeanParam PaginationParameters paginationParams, @QueryParam("query") String query) {

        PanacheQuery<Event> panacheQuery;

        // todo input and error handling
        if (query != null && !query.isBlank()) {
            QueryLexer lexer = new QueryLexer(CharStreams.fromString(query));
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            QueryParser parser = new QueryParser(tokens);
            QueryParser.QueryContext tree = parser.query();

            log.info("Parse Tree: " + tree.toStringTree(parser));

            JpqlQueryListener listener = new JpqlQueryListener();
            ParseTreeWalker walker = new ParseTreeWalker();
            walker.walk(listener, tree);

            String whereClause = listener.getJpqlWhereClause();
            Map<String, Object> parameters = listener.getParameters();

            log.info("Translated JPQL WHERE clause: '{}' with parameters: {}", whereClause, parameters);

            panacheQuery = Event.find(whereClause, parameters);
        } else {
            panacheQuery = Event.findAll();
        }

        List<EventRecord> events = panacheQuery.page(paginationParams.getPageIndex(), paginationParams.getPageSize())
                .project(EventRecord.class)
                .list();

        long count = panacheQuery.count();

        Page<EventRecord> page = RestUtils.toPage(events, paginationParams, count);

        return Response.ok(page)
                .header("X-Total-Count", count)
                .header("X-Page-Index", paginationParams.getPageIndex())
                .header("X-Page-Size", paginationParams.getPageSize())
                .build();
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
    public EventRecord getById(@PathParam("id") String eventId) {
        Event event = Event.findById(eventId); // NOSONAR

        if (event == null) {
            throw new NotFoundException("Event with id '{}' could not be found", eventId);
        }

        return mapper.toRecord(event);
    }

    @GET
    @Path("/{eventId}/history")
    @Operation(summary = "Get status history of an event")
    @APIResponse(
            responseCode = "200",
            description = "Status history",
            content = @Content(mediaType = MediaType.APPLICATION_JSON))
    @APIResponse(responseCode = "404", description = "Event not found")
    public List<EventStatusRecord> getStatusesForEvent(@PathParam("eventId") String eventId) {
        Event event = Event.findById(eventId); // NOSONAR

        if (event == null) {
            throw new NotFoundException("Event with id '{}' could not be found", eventId);
        }

        return mapper.toEventStatusRecords(event.getStatuses());
    }

    @GET
    @Path("/{eventId}/generations")
    @Operation(summary = "Get generations related to an event")
    @APIResponse(
            responseCode = "200",
            description = "Generation list",
            content = @Content(mediaType = MediaType.APPLICATION_JSON))
    @APIResponse(responseCode = "404", description = "Event not found")
    public List<GenerationRecord> getGenerationsForEvent(@PathParam("eventId") String eventId) {
        Event event = Event.findById(eventId); // NOSONAR

        if (event == null) {
            throw new NotFoundException("Event with id '{}' could not be found", eventId);
        }

        return mapper.toGenerationRecords(event.getGenerations());
    }

    @PATCH
    @Path("/{id}/status")
    @Operation(summary = "Update status of an event (Worker only)")
    @APIResponse(
            responseCode = "200",
            description = "Event status updated",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = EventRecord.class)))
    @APIResponse(responseCode = "404", description = "Event not found")
    @Transactional
    public Response updateEventStatus(
            @PathParam("id") String eventId,
            @NotNull @Valid EventStatusUpdatePayload payload) {

        Event event = Event.findById(eventId); // NOSONAR

        if (event == null) {
            throw new NotFoundException("Event with id '{}' could not be found", eventId);
        }

        event.setStatus(payload.status());
        event.setReason(payload.reason());
        event.save();

        EventRecord eventRecord = mapper.toRecord(event);

        Arc.container().beanManager().getEvent().fire(new EventStatusChangeEvent(eventRecord));

        return Response.ok(eventRecord).build();
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
            description = "Whether the retry should be considered a regeneration request (when set to true) or it should just regenerate failed generations and reuse successfully finished ones (when set to false)",
            schema = @Schema(type = SchemaType.BOOLEAN, defaultValue = "false"))
    @APIResponse(
            responseCode = "200",
            description = "Event content",
            content = @Content(mediaType = MediaType.APPLICATION_JSON))
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
    public EventRecord retry(@PathParam("id") String eventId, @QueryParam("force") boolean force) {
        log.info("Received new requested to retry event '{}' with force set to {}", eventId, force);

        log.trace("Fetching event '{}'", eventId);

        // TODO: ensure event is finished

        Event parentEvent = Event.findById(eventId); // NOSONAR

        if (parentEvent == null) {
            throw new NotFoundException("Event with id '{}' could not be found", eventId);
        }

        Event event = Event.builder()
                .withParent(parentEvent)
                .withCreated(Instant.now())
                .withMetadata(Map.of("source", Api.EVENT_TYPE))
                .build()
                .save();

        List<Generation> generations = new ArrayList<>();

        if (force) {
            // Rebuild all generations
            log.info("The force parameter was set, will red all generations assigned to this event");

            parentEvent.getGenerations().forEach(g -> {
                Generation generation = Generation.builder()
                        .withRequest(g.getRequest())
                        .withParent(g)
                        .withEvents(List.of(event))
                        .withReason("Created as a result of force retry of generation '" + g.getId() + "'")
                        .build()
                        .save();

                generations.add(generation);
            });
        } else {
            // Find failed generations and retry only these
            log.debug("Filtering generations from event {}, will retry failed ones", eventId);

            parentEvent.getGenerations().forEach(g -> {
                log.debug("Investigating generation '{}'", g.getId());

                Generation generation;

                if (g.getStatus() == GenerationStatus.FAILED) {
                    log.debug("Generation '{}' has failed stated, will retry", g.getId());

                    generation = Generation.builder()
                            .withRequest(g.getRequest())
                            .withParent(g)
                            .withEvents(List.of(event))
                            .withReason("Created as a result of retry of a failed generation '" + g.getId() + "'")
                            .build()
                            .save();
                } else {
                    log.debug("Generation '{}' is in correct state, will reuse", g.getId());

                    generation = g;
                }

                generations.add(generation);
            });
        }

        event.setGenerations(generations);
        event.save();

        log.info("New event created: {}", event.getId());

        EventRecord eventRecord = mapper.toRecord(event);

        Arc.container().beanManager().getEvent().fire(new EventStatusChangeEvent(eventRecord));

        return eventRecord;
    }
}
