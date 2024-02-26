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
package org.jboss.sbomer.core.dto.v1alpha3;

import java.time.Instant;
import java.util.Map;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import com.fasterxml.jackson.databind.JsonNode;

@Schema(name = "V1Alpha3SbomRecord")
public record SbomRecord(
        String id,
        String identifier,
        String rootPurl,
        Instant creationTime,
        @Schema(implementation = Map.class) JsonNode sbom,
        Integer configIndex,
        String statusMessage,
        SbomGenerationRequestRecord generationRequest) {

    public SbomRecord(
            String id,
            String identifier,
            String rootPurl,
            Instant creationTime,
            JsonNode sbom,
            Integer configIndex,
            String statusMessage,
            String gId,
            String gIdentifier,
            JsonNode gConfig,
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
                new SbomGenerationRequestRecord(
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