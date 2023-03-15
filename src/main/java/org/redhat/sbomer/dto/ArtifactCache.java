/**
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
package org.redhat.sbomer.dto;

import java.io.IOException;

import org.jboss.pnc.common.json.JsonUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@ToString
@EqualsAndHashCode
@JsonDeserialize(builder = ArtifactCache.Builder.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ArtifactCache {

    private final String id;

    private final String purl;

    private final JsonNode info;

    @lombok.Builder(builderClassName = "Builder", toBuilder = true)
    private ArtifactCache(String id, String purl, JsonNode info) {
        this.id = id;
        this.purl = purl;
        this.info = info;
    }

    @JsonIgnore
    public ArtifactInfo getArtifactInfo() {

        try {
            // Retrieved from DB
            if (info instanceof TextNode) {
                return JsonUtils.fromJson(info.asText(), ArtifactInfo.class);
            } else {
                // Not retrieved from DB
                String jsonString = JsonUtils.toJson(info);
                return JsonUtils.fromJson(jsonString, ArtifactInfo.class);
            }
        } catch (IOException e) {
            return ArtifactInfo.builder().build();
        }
    }

    @JsonPOJOBuilder(withPrefix = "")
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class Builder {
    }

}
