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
package org.jboss.sbomer.service.feature.sbom.k8s.model;

import java.util.Optional;

import org.jboss.sbomer.service.feature.sbom.model.RandomStringIdGenerator;

import io.fabric8.kubernetes.api.builder.VisitableBuilder;

public class GenerationRequestBuilder extends GenerationRequestFluentImpl<GenerationRequestBuilder>
        implements VisitableBuilder<GenerationRequest, GenerationRequestBuilder> {

    @Override
    public GenerationRequest build() {
        addToData(GenerationRequest.KEY_ID, RandomStringIdGenerator.generate());
        addToData(GenerationRequest.KEY_BUILD_ID, getBuildId());
        addToData(GenerationRequest.KEY_REASON, getReason());
        addToData(GenerationRequest.KEY_ENV_CONFIG, getConfig());
        addToData(GenerationRequest.KEY_CONFIG, getEnvConfig());
        addToData(GenerationRequest.KEY_REASON, getReason());
        addToData(
                GenerationRequest.KEY_STATUS,
                Optional.ofNullable(getStatus()).orElse(SbomGenerationStatus.NEW).name());

        GenerationRequest buildable = new GenerationRequest(
                getApiVersion(),
                getBinaryData(),
                getData(),
                getImmutable(),
                getKind(),
                buildMetadata());

        buildable.setAdditionalProperties(getAdditionalProperties());

        return buildable;
    }
}