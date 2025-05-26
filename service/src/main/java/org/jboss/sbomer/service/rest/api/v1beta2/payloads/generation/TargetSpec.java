package org.jboss.sbomer.service.rest.api.v1beta2.payloads.generation;

import jakarta.validation.constraints.NotNull;

// For target input
public record TargetSpec(@NotNull String identifier, @NotNull String type) {
}