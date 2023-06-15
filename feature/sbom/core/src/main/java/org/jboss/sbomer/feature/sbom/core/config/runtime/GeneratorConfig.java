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
package org.jboss.sbomer.feature.sbom.core.config.runtime;

import org.jboss.sbomer.core.enums.GeneratorType;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

/**
 * Configuration of the generator tool.
 */
@Data
@Builder
@Jacksonized
public class GeneratorConfig {
    /**
     * The selected generator.
     */
    GeneratorType type;
    /**
     * Optional custom arguments that should be passed to the generator.
     */
    String args;
    /**
     * Generator tool version that should be used.
     */
    String version;

}