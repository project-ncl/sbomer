/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2023 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.sbomer.service.v1beta2.generator;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

record GeneratorProfilesConfig(List<GeneratorProfile> generatorProfiles) {
}

/**
 * Defines the profile for a type of generator, including all its supported versions.
 *
 * @param name name of the generator
 * @param description optional description of the generator
 * @param versions list of supported/available versions of a given generator
 */
record GeneratorProfile(String name, String description, List<GeneratorVersionProfile> versions) {
}

/**
 * Defines the profile for a specific version of a generator.
 *
 * @param version the version of the Generator
 * @param supportedTypes list of types supported by this profile
 * @param supportedFormats list of manifest formats supported by this specific generator.
 * @param schema the JSON Schema for the config that the given version of the Generator supports. It is used to validate
 *        user input.
 * @param defaultResources default resource configuration for given Generator version
 *
 */
record GeneratorVersionProfile(String version, List<String> supportedTypes, List<String> supportedFormats,
        JsonNode schema, GeneratorResources defaultResources) {
}

/**
 * Represents the default resource allocation for a generator version.
 */
record GeneratorResources(ResourceRequirement requests, ResourceRequirement limits) {
}

/**
 * Represents resource requests or limits (CPU, memory). Used within GeneratorResources.
 */
record ResourceRequirement(String cpu, String memory) {
}

/**
 * Represents a single mapping from a content type to a list of preferred generators.
 *
 * @param type the type of the content to manifest, for example {@code CONTAINER_IMAGE} or {@code MAVEN_PROJECT}
 * @param generators a list of generators that support given type. First in the list is the preferred generator.
 */
record GeneratorMappingEntry(String type, List<String> generators) {
}

record GeneratorMappings(List<GeneratorMappingEntry> mappings) {
}
