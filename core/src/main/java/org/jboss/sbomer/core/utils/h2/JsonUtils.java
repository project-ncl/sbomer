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
package org.jboss.sbomer.core.utils.h2;

import org.jboss.sbomer.core.features.sbom.utils.ObjectMapperProvider;

import com.fasterxml.jackson.databind.JsonNode;

/*
 * Utility class which is needed to enhance the H2 database to query JsonNode content.
 *
 * This class is mapped in service/src/main/resources/init.sql
 */
public class JsonUtils {
    private JsonUtils() {
        throw new IllegalStateException("This is a utility class that should not be instantiated");
    }

    public static String jsonExtract(String json, String path) {
        try {
            if (json == null) {
                return null;
            }

            // Convert "$.field" to "field"
            String fieldName = path.replace("$.", "");

            JsonNode node = ObjectMapperProvider.json().readTree(json);
            JsonNode resultNode = node.get(fieldName);

            return resultNode != null ? resultNode.asText() : null;
        } catch (Exception e) {
            throw new RuntimeException("Invalid JSON or path", e);
        }
    }

}
