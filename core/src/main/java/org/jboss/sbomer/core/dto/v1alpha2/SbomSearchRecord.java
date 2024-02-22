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
package org.jboss.sbomer.core.dto.v1alpha2;

import java.time.Instant;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import com.fasterxml.jackson.databind.JsonNode;

@Schema(name = "V1Alpha2SbomSearchRecord")
public record SbomSearchRecord(
        String id,
        String buildId,
        String rootPurl,
        Instant creationTime,
        Integer configIndex,
        String statusMessage,
        SbomGenerationRequestRecord generationRequest) {

    public SbomSearchRecord(
            String id,
            String buildId,
            String rootPurl,
            Instant creationTime,
            Integer configIndex,
            String statusMessage,
            String gId,
            String gBuildId,
            JsonNode gConfig,
            Instant gCreationTime) {
        this(
                id,
                buildId,
                rootPurl,
                creationTime,
                configIndex,
                statusMessage,
                new SbomGenerationRequestRecord(gId, gBuildId, gConfig, gCreationTime));
    }

};
