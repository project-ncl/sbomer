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
package org.jboss.sbomer.core.test.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Collections;
import java.util.List;

import org.jboss.sbomer.core.features.sbom.utils.UrlUtils;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class UrlUtilsTest {

    @Nested
    class QualifiersTest {

        @Test
        void testHandleNullValues() {
            assertNull(UrlUtils.removeAllowedQualifiersFromPurl(null, null));
        }

        @Test
        void testHandleNullAllowList() {
            assertEquals(
                    "pkg:maven/org.jboss.arquillian.container/arquillian-container-impl-base@1.6.0.Final?type=jar",
                    UrlUtils.removeAllowedQualifiersFromPurl(
                            "pkg:maven/org.jboss.arquillian.container/arquillian-container-impl-base@1.6.0.Final?type=jar",
                            null));
        }

        @Test
        void testHandleEmptyAllowList() {
            assertEquals(
                    "pkg:maven/org.jboss.arquillian.container/arquillian-container-impl-base@1.6.0.Final?type=jar",
                    UrlUtils.removeAllowedQualifiersFromPurl(
                            "pkg:maven/org.jboss.arquillian.container/arquillian-container-impl-base@1.6.0.Final?type=jar",
                            Collections.emptyList()));
        }

        @Test
        void testHandleAllowList() {
            assertEquals(
                    "pkg:maven/org.apache.logging.log4j/log4j@2.19.0.redhat-00001?type=pom",
                    UrlUtils.removeAllowedQualifiersFromPurl(
                            "pkg:maven/org.apache.logging.log4j/log4j@2.19.0.redhat-00001?repository_url=https%3A%2F%2Fmaven.repository.redhat.com%2Fga%2F&type=pom",
                            List.of("repository_url")));
        }
    }

}
