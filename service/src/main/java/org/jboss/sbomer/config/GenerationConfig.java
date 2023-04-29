/**
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
package org.jboss.sbomer.config;

import java.util.Map;

import javax.enterprise.context.ApplicationScoped;

import org.jboss.sbomer.core.enums.GeneratorImplementation;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithName;

/**
 * @author Marek Goldmann
 */
@ApplicationScoped
@ConfigMapping(prefix = "sbomer.generation")
public interface GenerationConfig {
    public interface GeneratorConfig {
        String defaultVersion();

        String defaultArgs();

        default String version(String version) {
            if (version == null) {
                return defaultVersion();
            }

            return version;
        }

        default String args(String args) {
            if (args == null) {
                return defaultArgs();
            }

            return args;
        }
    }

    @WithName("enabled")
    boolean isEnabled();

    GeneratorImplementation defaultGenerator();

    Map<GeneratorImplementation, GeneratorConfig> generators();

    default GeneratorConfig forGenerator(GeneratorImplementation generator) {
        return generators().get(generator);
    }
}
