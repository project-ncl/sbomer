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

import org.jboss.sbomer.core.features.sbom.enums.GenerationRequestType;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@Data
@Builder
public class Sbom {
    public enum BomFormat {
        @JsonProperty("cyclonedx")
        CYCLONEDX
    }

    @Data
    @Builder
    public static class Bom {
        @Builder.Default
        BomFormat format = BomFormat.CYCLONEDX;
        String version;
        String link;
    }

    @Data
    @SuperBuilder
    @EqualsAndHashCode(callSuper = true)
    public static class OperationGenerationRequest extends GenerationRequest {
        Operation operation;
    }

    @Data
    @SuperBuilder
    @EqualsAndHashCode(callSuper = true)
    public static class PncBuildGenerationRequest extends GenerationRequest {
        Build build;
    }

    @Data
    @SuperBuilder
    @EqualsAndHashCode(callSuper = true)
    public static class ContainerImageGenerationRequest extends GenerationRequest {
        @JsonProperty("containerimage")
        Image containerImage;
    }

    @Data
    @SuperBuilder
    public abstract static class GenerationRequest {
        String id;
        GenerationRequestType type;
    }

    String id;
    String link;
    Bom bom;
    GenerationRequest generationRequest;
}
