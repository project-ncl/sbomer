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

import java.util.List;
import java.util.Map;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.sbomer.service.nextgen.core.dto.model.EventRecord;
import org.jboss.sbomer.service.nextgen.core.events.EventStatusChangeEvent;
import org.jboss.sbomer.service.nextgen.core.events.ResolveRequestEvent;
import org.jboss.sbomer.service.nextgen.core.payloads.management.ReplayRequest;
import org.jboss.sbomer.service.nextgen.core.utils.JacksonUtils;
import org.jboss.sbomer.service.nextgen.service.EntityMapper;
import org.jboss.sbomer.service.nextgen.service.model.Event;

import io.quarkus.arc.Arc;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import lombok.extern.slf4j.Slf4j;

@Path("/api/v1beta2/management")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "v1beta2")
@Slf4j
public class ManagementApi {

    @Inject
    EntityMapper mapper;

    @GET
    @Path("/event/resolvers")
    @Operation(summary = "Get supported event resolvers")
    @APIResponse(
            responseCode = "200",
            description = "List of supported resolvers",
            content = @Content(mediaType = MediaType.APPLICATION_JSON))
    @APIResponse(
            responseCode = "500",
            description = "Internal server error",
            content = @Content(mediaType = MediaType.APPLICATION_JSON))
    public List<String> listResolvers() {

        // TODO: dummy
        return List.of("et-advisory");
    }

    @POST
    @Path("/event/handle")
    @Operation(
            summary = "Initiate handling of an external event",
            description = "A way to manually process an external event supported by a given resolver.")
    @APIResponse(
            responseCode = "202",
            description = "Request accepted.",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = EventRecord.class)))
    @APIResponse(responseCode = "400", description = "Invalid request payload.")
    @APIResponse(responseCode = "500", description = "Internal server error.")
    @Transactional
    public Response handleExternalEvent(@NotNull @Valid ReplayRequest payload, @Context UriInfo uriInfo) {
        log.info(
                "Received request to handle external event via resolver of type '{}' and identifier: '{}'",
                payload.resolver(),
                payload.identifier());

        // Create an event
        Event event = Event.builder()
                // Convert payload to JsonNode
                .withRequest(JacksonUtils.toObjectNode(payload))
                .withMetadata(
                        Map.of(
                                EventsApi.KEY_SOURCE,
                                String.format("%s:%s", Api.EVENT_TYPE, uriInfo.getPath()),
                                EventsApi.KEY_RESOLVER,
                                payload.resolver(),
                                EventsApi.KEY_IDENTIFIER,
                                payload.identifier()))
                .withReason("Created as a result of a REST API call")
                .build()
                .save();

        // Convert to DTO
        EventRecord eventRecord = mapper.toRecord(event);

        // Fire an event so that resolver could handle it
        Arc.container().beanManager().getEvent().fire(new EventStatusChangeEvent(eventRecord));

        // And fire an additional event that encapsulates the information that this is a request for resolution
        Arc.container().beanManager().getEvent().fire(new ResolveRequestEvent(eventRecord));

        // Return DTO to user
        return Response.status(Response.Status.ACCEPTED).entity(eventRecord).build();
    }

}
