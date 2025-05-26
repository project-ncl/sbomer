package org.jboss.sbomer.service.rest.api.v1beta2.payloads.generation;

import java.util.List;

import org.jboss.sbomer.service.feature.sbom.model.v1beta2.dto.EventRecord;
import org.jboss.sbomer.service.feature.sbom.model.v1beta2.dto.GenerationRecord;

import jakarta.validation.constraints.NotNull;

/**
 * <p>
 * Representation of the response after requesting generations.
 * </p>
 *
 * @param event Event related to the work information.
 * @param generations List of generations.
 */
public record GenerationsResponse(EventRecord event, @NotNull List<GenerationRecord> generations) {
}