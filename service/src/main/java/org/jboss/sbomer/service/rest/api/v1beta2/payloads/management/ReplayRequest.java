package org.jboss.sbomer.service.rest.api.v1beta2.payloads.management;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;

@Schema(description = "Payload to request the replay of an external event to be handled by a particular resolver.")
public record ReplayRequest(
        @NotBlank @Schema(
                description = "Identifier of the resolver type which supports particular external event.",
                example = "et-advisory") String resolver,

        @NotBlank @Schema(
                description = "The unique identifier of the external event known to particular resolver.",
                example = "1234") String identifier,

        @Schema(
                description = "Reason for initiating this replay. For audit purposes.",
                example = "Original event missed during system maintenance on 2024-01-10.") String reason) {
}