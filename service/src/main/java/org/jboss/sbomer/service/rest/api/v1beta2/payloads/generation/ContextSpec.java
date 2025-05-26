package org.jboss.sbomer.service.rest.api.v1beta2.payloads.generation;

import java.time.Instant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * <p>
 * The original context.
 * </p>
 *
 * <p>
 * In case we are responding to an external event that should result in generation(s), we can encapsulate this
 * information using {@link ContextSpec}.
 * </p>
 *
 * @param eventId Original event identifier. This is NOT the SBOMer event identifier.
 * @param system What is the source system of this event.
 * @param receivedAt What is the date and time when we initially received it.
 */
public record ContextSpec(@NotBlank String eventId, String system, @NotNull Instant receivedAt, String payload) {
}