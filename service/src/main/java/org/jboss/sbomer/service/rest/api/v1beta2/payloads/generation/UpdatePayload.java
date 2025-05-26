package org.jboss.sbomer.service.rest.api.v1beta2.payloads.generation;

import java.util.List;

import jakarta.validation.constraints.NotNull;

/**
 * <p>
 * Payload used to update SBOMer with information about the progress of the generation.
 * </p>
 *
 * <p>
 * This endpoint is used only by workers (generators).
 * </p>
 *
 * @param status The status identifier.
 * @param result A programmatic result information.
 * @param reason A human-readable description of the current status.
 * @param manifests List of manifest identifiers.
 */
public record UpdatePayload(@NotNull String status, String result, String reason, List<String> manifests

) {
}