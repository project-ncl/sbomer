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
