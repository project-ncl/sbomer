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
package org.jboss.sbomer.service.feature.sbom.errata.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ErrataVariant {

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ErrataProduct {
        private Long id;
        private String name;
        @JsonProperty("short_name")
        private String shortName;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ErrataProductVersion {
        private Long id;
        private String name;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Attributes {
        private String name;
        private String description;
        private String cpe;
        private Relationships relationships;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RhelRelease {
        private Long id;
        private String name;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RhelVariant {
        private Long id;
        private String name;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Relationships {
        private ErrataProduct product;
        @JsonProperty("product_version")
        private ErrataProductVersion productVersion;
        @JsonProperty("rhel_release")
        private RhelRelease rhelRelease;
        @JsonProperty("rhel_variant")
        private RhelVariant rhelVariant;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class VariantData {
        private Long id;
        private String type;
        private Attributes attributes;
    }

    private VariantData data;
}
