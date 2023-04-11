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
package org.jboss.sbomer.cli.model;

import org.jboss.sbomer.core.enums.GeneratorImplementation;
import org.jboss.sbomer.core.enums.ProcessorImplementation;
import org.jboss.sbomer.core.enums.SbomType;

import com.fasterxml.jackson.databind.JsonNode;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * This is a just-enough representation of the {@link org.jboss.sbomer.model.Sbom} class that is required for
 * processing. This is used by the {@link org.jboss.sbomer.cli.client.SBOMerClient} REST client.
 */
@Getter
@Setter
@ToString
public class Sbom {
    private Long id;
    private String buildId;
    private JsonNode sbom;
    private GeneratorImplementation generator;
    private ProcessorImplementation processor;
    private SbomType type;
}
