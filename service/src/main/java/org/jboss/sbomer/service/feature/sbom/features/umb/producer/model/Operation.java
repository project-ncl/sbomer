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
package org.jboss.sbomer.service.feature.sbom.features.umb.producer.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Operation {
    public static enum BuildSystem {
        @JsonProperty("pnc")
        PNC
    }

    /**
     * Type of the build system.
     */
    @JsonProperty("system")
    @Builder.Default
    BuildSystem buildSystem = BuildSystem.PNC;

    /**
     * The unique operation identifier.
     */
    String id;

    /**
     * Link to the build system's API for the operation.
     */
    String link;

    /**
     * The identifier of the deliverable.
     */
    String deliverable;
}
