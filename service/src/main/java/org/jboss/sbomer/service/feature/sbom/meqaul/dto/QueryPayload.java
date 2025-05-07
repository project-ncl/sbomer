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
package org.jboss.sbomer.service.feature.sbom.meqaul.dto;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.Data;

@Data
public class QueryPayload {

    private String bomFilePath;

    @JsonProperty("query")
    private final String query;

    @JsonProperty("input")
    private final String input;

    public static QueryPayload fromBomFilePath(String query, String bomFilePath) throws IOException {
        try {
            Path p = Paths.get(bomFilePath);
            String bomContents = new String(Files.readAllBytes(p));
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode fileJsonNode = objectMapper.readTree(bomContents);
            ObjectNode parentNode = objectMapper.createObjectNode();
            parentNode.set("input", fileJsonNode);
            return new QueryPayload(query, objectMapper.writeValueAsString(parentNode));
        } catch (IOException e) {
            throw new IOException("Error while processing file: " + bomFilePath, e);
        }
    }
}
