package org.jboss.sbomer.core.test.unit.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import org.jboss.sbomer.core.SchemaValidator.ValidationResult;
import org.jboss.sbomer.core.config.RequestConfigSchemaValidator;
import org.jboss.sbomer.core.config.request.ErrataAdvisoryRequestConfig;
import org.jboss.sbomer.core.config.request.RequestConfig;
import org.jboss.sbomer.core.test.TestResources;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class RequestConfigSchemaValidatorTest {
    final RequestConfigSchemaValidator requestConfigValidator = new RequestConfigSchemaValidator();

    @Nested
    class ErrataAdvisoryRequestConfigTests {

        @Test
        void validateConfig() throws IOException {
            String content = TestResources.asString("configs/advisory.json");

            ErrataAdvisoryRequestConfig config = RequestConfig.fromString(content, ErrataAdvisoryRequestConfig.class);

            assertEquals("sbomer.jboss.org/v1alpha1", config.getApiVersion());
            assertEquals("12345", config.getAdvisoryId());
            // verify that forceBuild defaults to false
            assertEquals(false, config.isForceBuild());

            ValidationResult result = requestConfigValidator.validate(config);
            assertTrue(result.isValid());
            assertTrue(result.getErrors().isEmpty());
        }

        @Test
        void validateConfigWithForceBuild() throws IOException {
            String content = TestResources.asString("configs/advisory-force-build.json");

            ErrataAdvisoryRequestConfig config = RequestConfig.fromString(content, ErrataAdvisoryRequestConfig.class);

            assertEquals("sbomer.jboss.org/v1alpha1", config.getApiVersion());
            assertEquals("12345", config.getAdvisoryId());
            // verify that forceBuild is true
            assertEquals(true, config.isForceBuild());

            ValidationResult result = requestConfigValidator.validate(config);
            assertTrue(result.isValid());
            assertTrue(result.getErrors().isEmpty());
        }

    }

}
