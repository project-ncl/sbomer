package org.jboss.sbomer.service.rest.api.v1beta2;

import java.util.Map;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.sbomer.core.features.sbom.utils.ObjectMapperProvider;
import org.jboss.sbomer.service.feature.sbom.model.v1beta2.Event;
import org.jboss.sbomer.service.feature.sbom.model.v1beta2.EventStatusHistory;
import org.jboss.sbomer.service.feature.sbom.model.v1beta2.dto.EventRecord;
import org.jboss.sbomer.service.feature.sbom.model.v1beta2.dto.V1Beta2Mapper;
import org.jboss.sbomer.service.feature.sbom.model.v1beta2.enums.EventType;

import com.fasterxml.jackson.databind.JsonNode;

import io.quarkus.arc.Arc;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;

@Schema(description = "Payload to request the replay of an external event to be handled by a particular resolver.")
record ReplayRequest(
        @NotBlank @Schema(
                description = "Identifier for the resolver type responsible for this kind of external event.",
                example = "et-advisory") String resolver,

        @NotBlank @Schema(
                description = "The unique identifier the event known to  particular resolver.",
                example = "1234") String identifier,

        @Schema(
                description = "Reason for initiating this replay. For audit purposes.",
                example = "Original event missed during system maintenance on 2024-01-10.") String reason) {
}

@Path("/api/v1beta2/management")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "v1beta2")
@Slf4j
public class ManagementApi {

    @Inject
    V1Beta2Mapper mapper;

    @POST
    @Path("/events/replay")
    @Operation(
            summary = "Initiate replay of an external event",
            description = "Requests SBOMer to command a specified listener type to reprocess a given external event. "
                    + "This creates an initial 'Replay Initiation Event' in SBOMer to track this request. "
                    + "The ID of this created Event is returned and serves as the primary handle for tracking the overall reprocessing flow.")
    @APIResponse(
            responseCode = "202",
            description = "Replay initiation request accepted. The returned EventRecord tracks this initiation step.",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = EventRecord.class)))
    @APIResponse(
            responseCode = "400",
            description = "Invalid request payload (e.g., missing listenerType or externalEventId).")
    @APIResponse(
            responseCode = "500",
            description = "Internal server error (e.g., failed to dispatch command to listener queue).")
    @Transactional
    public Response replayExternalEvent(@NotNull @Valid ReplayRequest payload) {
        log.info(
                "Received request to replay external event via listener type '{}' for external eventId '{}'",
                payload.resolver(),
                payload.identifier());

        JsonNode request = null;

        try {
            request = ObjectMapperProvider.json().valueToTree(payload);
        } catch (IllegalArgumentException e) {
            log.error(
                    "Critical error: Failed to convert ReplayExternalEventRequest DTO to JsonNode. This should not happen with valid DTOs.",
                    e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\":\"Internal error processing request payload.\"}")
                    .build();
        }

        Event event = Event.builder()
                // .withEventType("EXTERNAL_EVENT_REPLAY_INITIATED") // Specific type for this action
                .withEvent(request)
                .withMetadata(
                        Map.of(
                                EventsV1Beta2.KEY_SOURCE,
                                String.format("%s:/api/v1beta2/management", EventType.REST.toName()),
                                EventsV1Beta2.KEY_RESOLVER,
                                payload.resolver(),
                                EventsV1Beta2.KEY_IDENTIFIER,
                                payload.identifier()))
                .build()
                .save();

        new EventStatusHistory(event, event.getStatus().name(), "Initial creation").save();

        EventRecord eventRecord = mapper.toRecord(event);

        Arc.container().beanManager().getEvent().fire(eventRecord);

        return Response.status(Response.Status.ACCEPTED).entity(eventRecord).build();
    }

}
