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
