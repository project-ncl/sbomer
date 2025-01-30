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
package org.jboss.sbomer.core.config;

import java.util.List;

import org.jboss.sbomer.core.features.sbom.enums.ProcessorType;

import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

/**
 * @author Marek Goldmann
 */
public interface DefaultProcessingConfig {
    interface ProcessorConfig {
        ProcessorType name();
    }

    /**
     * Whether the processing feature should be enabled.
     *
     * @return {@code true} if enabled, {@code false} otherwise
     */
    @WithDefault("true")
    @WithName("enabled")
    boolean isEnabled();

    /**
     * Whether the default configured processors should be run automatically for any base SBOM generated.
     *
     * @return {@code true} if processing should be run automatically, {@code false} otherwise
     */
    @WithDefault("true")
    @WithName("auto-process")
    boolean shouldAutoProcess();

    /**
     * List of configured default processors.
     *
     * @return
     */
    List<ProcessorConfig> defaultProcessors();

}
