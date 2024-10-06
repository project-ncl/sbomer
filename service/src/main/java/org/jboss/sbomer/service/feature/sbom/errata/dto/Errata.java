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

import java.time.Instant;
import java.util.Optional;

import org.jboss.sbomer.core.features.sbom.utils.ObjectMapperProvider;
import org.jboss.sbomer.service.feature.sbom.errata.dto.enums.ErrataStatus;
import org.jboss.sbomer.service.feature.sbom.errata.dto.enums.ErrataType;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
@Builder(setterPrefix = "with")
@NoArgsConstructor
@AllArgsConstructor
public class Errata {

    @Data
    @Builder(setterPrefix = "with")
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class WrappedErrata {
        Rhba rhba;
        Rhea rhea;
        Rhsa rhsa;
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    @SuperBuilder(setterPrefix = "with")
    public static class Rhba extends Details {
        public Rhba() {
        }
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    @SuperBuilder(setterPrefix = "with")
    public static class Rhea extends Details {
        public Rhea() {
        }
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    @SuperBuilder(setterPrefix = "with")
    public static class Rhsa extends Details {
        public Rhsa() {
        }
    }

    @Data
    @SuperBuilder(setterPrefix = "with")
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Details {
        Long id;
        String fulladvisory;
        String synopsis;
        ErrataStatus status;
        ErrataProduct product;
        @JsonProperty("is_brew")
        Boolean brew;
        @JsonProperty("group_id")
        Long groupId;
        @JsonProperty("old_advisory")
        String oldAdvisory;
        @JsonProperty("text_only")
        Boolean textonly;
        @JsonProperty("content_types")
        String[] contentTypes;
        @JsonProperty("created_at")
        Instant createdAt;
        @JsonProperty("actual_ship_date")
        Instant actualShipDate;
    }

    @Data
    @Builder(setterPrefix = "with")
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ErrataProduct {
        Long id;
        String name;
        @JsonProperty("short_name")
        String shortName;
    }

    @Data
    @Builder(setterPrefix = "with")
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WrappedContent {
        Content content;
    }

    @Data
    @Builder(setterPrefix = "with")
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Content {
        @JsonProperty("id")
        Long id;
        @JsonProperty("errata_id")
        Long errataId;
        String topic;
        String description;
        String solution;
        String cve;
        @JsonProperty("how_to_test")
        String notes;
        @JsonProperty("updated_at")
        Instant updatedAt;
        @JsonProperty("text_only_cpe")
        String textOnlyCpe;
        @JsonProperty("product_version_text")
        String productVersionText;
    }

    WrappedErrata errata;

    @JsonProperty("original_type")
    ErrataType originalType;

    WrappedContent content;

    public Optional<Details> getDetails() {
        switch (originalType) {
            case RHSA:
                return Optional.of(errata.rhsa);
            case RHBA:
                return Optional.of(errata.rhba);
            case RHEA:
                return Optional.of(errata.rhea);
            default:
                return Optional.empty();
        }
    }

    public Optional<JsonNode> getNotesMapping() {

        // Check if the string is null or empty
        if (content.content.notes == null || content.content.notes.trim().isEmpty()) {
            return Optional.empty();
        }

        try {
            // Try to parse the string into a JsonNode
            return Optional.of(ObjectMapperProvider.json().readTree(content.content.notes));
        } catch (Exception e) {
            log.info("The erratum does not contain a notes content with JSON text", e.getMessage());
            return Optional.empty();
        }
    }
}