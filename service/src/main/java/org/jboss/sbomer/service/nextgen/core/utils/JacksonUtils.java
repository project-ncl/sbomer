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
package org.jboss.sbomer.service.nextgen.core.utils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.jboss.sbomer.core.errors.ApplicationException;
import org.jboss.sbomer.core.features.sbom.utils.ObjectMapperProvider;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JacksonUtils {
    private JacksonUtils() {
        throw new IllegalStateException("This is a utility class that should not be instantiated");
    }

    /**
     * Converts the {@link ObjectNode} into an object representation of a given class.
     *
     * @param clazz The class the object should be converted to
     * @param node The content that should be converted
     * @return Converted object.
     */
    public static <T> T parse(Class<T> clazz, ObjectNode node) {
        try {
            return ObjectMapperProvider.json().treeToValue(node, clazz);
        } catch (JsonProcessingException e) {
            throw new ApplicationException("Unable to convert provided content ino a {} object", clazz.getName(), e);
        }
    }

    public static ObjectNode toObjectNode(Object payload) {
        try {
            return ObjectMapperProvider.json().convertValue(payload, ObjectNode.class);
        } catch (IllegalArgumentException e) {
            log.error("Failed to convert object: '{}' to JsonNode", payload, e);
            throw new ApplicationException("Failed to convert given object to JsonNode", e);
        }
    }

    /**
     * Merges two {@link JsonNode} objects that are in fact {@link ObjectNode}s.
     *
     * @param mainNode The main object.
     * @param updateNode The secondary object which should be merged with the main one.
     * @return Updated main object.
     */
    public static JsonNode merge(JsonNode mainNode, JsonNode updateNode) {
        if (mainNode.isObject() && updateNode.isObject()) {
            ObjectNode mainObjectNode = (ObjectNode) mainNode;
            Iterator<Map.Entry<String, JsonNode>> updateFields = updateNode.fields();

            while (updateFields.hasNext()) {
                Map.Entry<String, JsonNode> fieldEntry = updateFields.next();
                String fieldName = fieldEntry.getKey();
                JsonNode value = fieldEntry.getValue();

                if (mainObjectNode.has(fieldName) && mainObjectNode.get(fieldName).isObject() && value.isObject()) {
                    // Recursive merge for nested objects
                    merge(mainObjectNode.get(fieldName), value);
                } else {
                    // Overwrite or add the field
                    mainObjectNode.set(fieldName, value);
                }
            }
        }
        return mainNode;
    }

    /**
     * Reads manifests at given {@code paths} as {@link JsonNode}s.
     *
     * @param paths List of {@link Path}s to manifests in JSON format.
     * @return List of {@link JsonNode}s.
     */
    public static List<JsonNode> readBoms(List<Path> paths) {
        List<JsonNode> boms = new ArrayList<>();

        log.info("Reading {} files...", paths.size());

        for (Path manifestPath : paths) {
            log.debug("Reading manifest at path '{}'...", manifestPath);

            JsonNode bom;

            try {
                bom = ObjectMapperProvider.json().readTree(manifestPath.toFile());
            } catch (IOException e) {
                throw new ApplicationException("Could not read the manifest at '{}'", manifestPath.toAbsolutePath(), e);
            }

            if (bom == null) {
                throw new ApplicationException("Manifest at path '{}' is empty", manifestPath.toAbsolutePath());
            }
        }

        return boms;
    }
}
