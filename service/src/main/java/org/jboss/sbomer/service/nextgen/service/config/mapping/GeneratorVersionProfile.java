package org.jboss.sbomer.service.nextgen.service.config.mapping;

import java.util.List;

import org.jboss.sbomer.service.nextgen.core.payloads.generation.GeneratorConfigSpec;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Defines the profile for a specific version of a generator.
 *
 * @param version the version of the Generator
 * @param supportedTargetTypes list of types supported by this profile
 * @param supportedFormats list of manifest formats supported by this specific generator.
 * @param schema the JSON Schema for the config that the given version of the Generator supports. It is used to validate
 *        user input.
 * @param defaultResources default resource configuration for given Generator version
 *
 */
public record GeneratorVersionProfile(String version, List<String> supportedTargetTypes, List<String> supportedFormats,
        JsonNode schema, GeneratorConfigSpec defaultConfig) {
}