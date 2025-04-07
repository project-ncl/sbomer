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
package org.jboss.sbomer.core.dto.v1beta1;

import java.time.Instant;
import java.util.Map;

import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.jboss.sbomer.core.features.sbom.config.Config;

import com.fasterxml.jackson.databind.JsonNode;

@Schema(name = "V1Beta1SbomRecord")
public record V1Beta1ManifestRecord(String id, String identifier, String rootPurl, Instant creationTime,
        @Schema(implementation = Map.class) JsonNode sbom, Integer configIndex, String statusMessage,
        V1Beta1GenerationRecord generation) {

    public V1Beta1ManifestRecord(
            String id,
            String identifier,
            String rootPurl,
            Instant creationTime,
            JsonNode sbom,
            Integer configIndex,
            String statusMessage,
            String gId,
            String gIdentifier,
            Config gConfig,
            String gType,
            Instant gCreationTime,
            String gStatus,
            String gResult,
            String gReason) {
        this(
                id,
                identifier,
                rootPurl,
                creationTime,
                sbom,
                configIndex,
                statusMessage,
                new V1Beta1GenerationRecord(
                        gId,
                        gIdentifier,
                        gConfig,
                        gType,
                        gCreationTime,
                        gStatus,
                        gResult,
                        gReason));

    }
}
