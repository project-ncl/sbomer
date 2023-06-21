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
package org.jboss.sbomer.core.test.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.cyclonedx.model.Bom;
import org.jboss.sbomer.core.features.sbom.utils.SbomUtils;
import org.jboss.sbomer.core.test.TestResources;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;

public class SbomUtilsTest {

    static Path sbomPath(String fileName) {
        return Paths.get("src", "test", "resources", "sboms", fileName);
    }

    @Nested
    @DisplayName("SbomUtils")
    class SbomUtilsTestNested {
        @Test
        void shouldReadSbomFromFile() throws Exception {
            Bom bom = SbomUtils.fromPath(sbomPath("base.json"));

            assertEquals(39, bom.getComponents().size());

            assertEquals(
                    "Apache-2.0",
                    bom.getMetadata().getComponent().getLicenseChoice().getLicenses().get(0).getId());
        }

        @Test
        void shouldReadSbomFromString() throws Exception {
            String bomStr = TestResources.asString(sbomPath("base.json"));
            Bom bom = SbomUtils.fromString(bomStr);

            assertEquals(39, bom.getComponents().size());

            assertEquals(
                    "Apache-2.0",
                    bom.getMetadata().getComponent().getLicenseChoice().getLicenses().get(0).getId());
        }

        @Test
        void shouldReadFromFileAndConvertToJsonNode() {
            Bom bom = SbomUtils.fromPath(sbomPath("base.json"));
            JsonNode jsonNode = SbomUtils.toJsonNode(bom);
            Bom bom2 = SbomUtils.fromJsonNode(jsonNode);
            assertEquals(bom, bom2);

            JsonNode licenses = jsonNode.get("metadata").get("component").get("licenses");

            assertNotNull(licenses);
            assertEquals(JsonNodeType.ARRAY, licenses.getNodeType());

            JsonNode license = licenses.get(0);
            assertNotNull(license);
            assertEquals("Apache-2.0", license.get("license").get("id").asText());
        }
    }

}
