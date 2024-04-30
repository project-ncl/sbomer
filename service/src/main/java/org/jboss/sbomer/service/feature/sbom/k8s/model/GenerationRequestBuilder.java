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

import org.jboss.sbomer.core.features.sbom.enums.GenerationRequestType;
import org.jboss.sbomer.service.feature.sbom.k8s.resources.Labels;
import org.jboss.sbomer.service.feature.sbom.model.RandomStringIdGenerator;

import io.fabric8.kubernetes.api.builder.VisitableBuilder;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;

public class GenerationRequestBuilder extends GenerationRequestFluent<GenerationRequestBuilder>
        implements VisitableBuilder<GenerationRequest, GenerationRequestBuilder> {

    public GenerationRequestBuilder(GenerationRequestType type) {
        withId(RandomStringIdGenerator.generate());
        withType(type);
    }

    @Override
    public GenerationRequest build() {
        withNewMetadataLike(
                new ObjectMetaBuilder().withName("sbom-request-" + getId().toLowerCase())
                        .withLabels(Labels.defaultLabelsToMap(getType()))
                        .build())
                .endMetadata();

        addToData(GenerationRequest.KEY_ID, getId());
        addToData(GenerationRequest.KEY_IDENTIFIER, getIdentifier());
        addToData(GenerationRequest.KEY_REASON, getReason());
        addToData(GenerationRequest.KEY_ENV_CONFIG, getEnvConfig());
        addToData(GenerationRequest.KEY_CONFIG, getConfig());
        addToData(GenerationRequest.KEY_REASON, getReason());
        addToData(
                GenerationRequest.KEY_STATUS,
                Optional.ofNullable(getStatus()).orElse(SbomGenerationStatus.NEW).name());

        // If the SbomGenerationType is null, default to SbomGenerationType.BUILD
        addToData(
                GenerationRequest.KEY_TYPE,
                Optional.ofNullable(getType()).orElse(GenerationRequestType.BUILD).toName());

        GenerationRequest buildable = new GenerationRequest(
                getApiVersion(),
                getBinaryData(),
                getData(),
                getImmutable(),
                HasMetadata.getKind(ConfigMap.class),
                buildMetadata());

        buildable.setAdditionalProperties(getAdditionalProperties());

        return buildable;
    }
}
