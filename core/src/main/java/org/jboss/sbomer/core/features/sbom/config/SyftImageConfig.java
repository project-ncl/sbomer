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
package org.jboss.sbomer.core.features.sbom.config;

import java.util.ArrayList;
import java.util.List;

import org.jboss.sbomer.core.features.sbom.config.runtime.DefaultProcessorConfig;
import org.jboss.sbomer.core.features.sbom.config.runtime.ProcessorConfig;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeName;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

@Data
@SuperBuilder(setterPrefix = "with")
@Jacksonized
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonTypeName("syft-image")
public class SyftImageConfig extends Config {
    /**
     * List of paths within the container image filesystem. If populated, only components located under given paths will
     * be added to manifest.
     */
    @Builder.Default
    List<String> paths = new ArrayList<>();

    /**
     * Flag to indicate whether RPMs should be added to manifest.
     */
    @Builder.Default
    boolean rpms = false;

    /**
     * Processors configuration.
     */
    @Builder.Default
    List<ProcessorConfig> processors = new ArrayList<>();

    @JsonIgnore
    @Override
    public boolean isEmpty() {
        return this.equals(new SyftImageConfig());
    }

    @Override
    @JsonIgnore
    protected List<String> processCommand() {
        List<String> command = new ArrayList<>();

        DefaultProcessorConfig defaultProcessorConfig = new DefaultProcessorConfig();

        // If the default processor is not there, add it.
        // This ensures that even after we initialize the object, for examle after deserialziation,
        // we will have the default processor added, so that the correct command can be instantiated.
        if (!processors.contains(defaultProcessorConfig)) {
            processors.add(0, defaultProcessorConfig);
        }

        processors.forEach(processor -> command.addAll(processor.toCommand()));

        return command;
    }
}
