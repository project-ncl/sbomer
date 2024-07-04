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
package org.jboss.sbomer.core.features.sbom.config.runtime;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.jboss.sbomer.core.features.sbom.config.OperationConfig;
import org.jboss.sbomer.core.features.sbom.config.PncBuildConfig;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

/**
 * Product configuration.
 *
 * @author Marek Goldmann
 */
@Data
@Builder(setterPrefix = "with")
@Jacksonized
public class ProductConfig {
    /**
     * Processors configuration.
     */
    @Builder.Default
    List<ProcessorConfig> processors = new ArrayList<>();

    /**
     * Generator configuration.
     */
    GeneratorConfig generator;

    @JsonIgnore
    public boolean hasDefaultProcessor() {
        return Optional.ofNullable(processors)
                .map(Collection::stream)
                .orElseGet(Stream::empty)
                .anyMatch(DefaultProcessorConfig.class::isInstance);
    }

    @JsonIgnore
    public List<String> generateCommand(PncBuildConfig config) {
        List<String> command = new ArrayList<>();

        // We're running the command under the sbomer feature.
        command.add("-v");
        command.add("sbom");
        command.add("generate");

        command.add("--build-id");
        command.add(config.getBuildId());

        // TODO: Make this somewhat static
        command.add(generator.getType().toString().toLowerCase().replace("_", "-"));

        if (generator.getVersion() != null) {
            command.add("--tool-version");
            command.add(generator.getVersion());
        }

        if (generator.getArgs() != null) {
            command.add("--tool-args");
            command.add(generator.getArgs());
        }

        if (!processors.isEmpty()) {
            command.add("process");

            processors.forEach(processor -> command.addAll(processor.toCommand()));
        }

        return command;
    }

    @JsonIgnore
    public List<String> generateCommand(OperationConfig config) {
        List<String> command = new ArrayList<>();

        // We're running the command under the sbomer feature.
        command.add("-v");
        command.add("sbom");
        command.add("generate-operation");

        command.add("--operation-id");
        command.add(config.getOperationId());

        command.add(generator.getType().toString().toLowerCase().replace("_", "-"));

        return command;
    }
}
