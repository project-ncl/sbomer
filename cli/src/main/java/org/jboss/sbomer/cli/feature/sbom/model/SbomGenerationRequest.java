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
package org.jboss.sbomer.cli.feature.sbom.model;

import java.time.Instant;

import org.jboss.sbomer.core.features.sbom.config.Config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

/**
 * This is a just-enough representation of the {@link org.jboss.sbomer.service.feature.sbom.model.SbomGenerationRequest}
 * class that is required for processing. This is used by the
 * {@link org.jboss.sbomer.cli.feature.sbom.client.SBOMerClient} REST client. As we are only interested in the
 * {@code GenerationResult.SUCCESS} and {@code SbomGenerationStatus.FINISHED}, there is no real point in mapping them
 * here
 */
@Data
@Builder
@Jacksonized
@JsonIgnoreProperties(ignoreUnknown = true)
// We use lombok to generate setters and getters
@SuppressWarnings("java:S1068")
public class SbomGenerationRequest {

    private String id;
    private String identifier;
    private String type;
    private Config config;
    private Instant creationTime;

}
