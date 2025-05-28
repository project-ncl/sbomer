package org.jboss.sbomer.service.rest.api.v1beta2.payloads.generation;

import java.util.Map;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * Options for a given generator used for generating manifests.
 * 
 * @param format The manifest output format.
 * @param resources Resource requirements related to execution phase for a current generation.
 * @param options Custom, generator(version)-specific, options which should be applied to the generation process.
 */
public record GeneratorConfigSpec(String format, ResourcesSpec resources,
        @Schema(description = "Specific options for a particular generator version.") Map<String, Object> options) {
}