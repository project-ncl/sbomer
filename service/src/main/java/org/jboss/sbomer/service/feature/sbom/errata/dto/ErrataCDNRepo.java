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

import lombok.Data;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ErrataCDNRepo {
    private long id;
    private String type;
    private Attributes attributes;
    private Relationships relationships;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Attributes {
        private String name;
        @JsonProperty("release_type")
        private String releaseType;
        @JsonProperty("use_for_tps")
        private boolean useForTps;
        @JsonProperty("tps_stream")
        private String tpsStream;
        @JsonProperty("content_type")
        private String contentType;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Relationships {
        private Arch arch;
        private List<Variant> variants;

        @Data
        public static class Arch {
            private long id;
            private String name;
        }

        @Data
        public static class Variant {
            private long id;
            private String name;
        }
    }
}