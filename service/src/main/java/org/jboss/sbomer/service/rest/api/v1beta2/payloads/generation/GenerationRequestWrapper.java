package org.jboss.sbomer.service.rest.api.v1beta2.payloads.generation;

import java.util.Map;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * <p>
 * Configuration of CPU and memory for a given execution.
 * </p>
 *
 */
@JsonIgnoreProperties(ignoreUnknown = true)
record ResourceRequirementSpec(String cpu, String memory) {
}

/**
 * <p>
 * Generator configuration.
 * </p>
 *
 * @param name Name of the generator.
 * @param version Version of the generator.
 * @param options Custom, generator(version)-specific, options which should be applied to the generation process.
 */
record GeneratorOptionsSpec(String name, String version,
        @Schema(description = "Generator-specific options") Map<String, Object> options) {
}

@JsonIgnoreProperties(ignoreUnknown = true)
public record GenerationRequestWrapper(@NotNull @Valid TargetSpec target, @Valid ConfigSpec config) {
}
