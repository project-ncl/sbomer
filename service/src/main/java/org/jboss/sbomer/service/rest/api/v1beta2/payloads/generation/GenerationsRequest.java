package org.jboss.sbomer.service.rest.api.v1beta2.payloads.generation;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * <p>
 * Representation of the payload when requesting generations.
 * </p>
 *
 * @param context Optional context related to the external generation request event.
 * @param requests List of generation requests.
 */
public record GenerationsRequest(@Valid ContextSpec context, @NotNull List<GenerationRequestSpec> requests) {
}