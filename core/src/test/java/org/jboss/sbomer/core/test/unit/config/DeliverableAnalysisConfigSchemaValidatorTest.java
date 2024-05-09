package org.jboss.sbomer.core.test.unit.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.jboss.sbomer.core.SchemaValidator.ValidationResult;
import org.jboss.sbomer.core.config.DeliverableAnalysisConfigSchemaValidator;
import org.jboss.sbomer.core.errors.ApplicationException;
import org.jboss.sbomer.core.features.sbom.config.runtime.DeliverableAnalysisConfig;
import org.jboss.sbomer.core.features.sbom.config.runtime.ErrataConfig;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class DeliverableAnalysisConfigSchemaValidatorTest {
    DeliverableAnalysisConfigSchemaValidator validator = new DeliverableAnalysisConfigSchemaValidator();

    private DeliverableAnalysisConfig minimalDeliverableAnalysisConfig() {
        String milestoneId = "13";
        List<String> urls = List.of("http://myurl1.com", "http://myurl2.com");

        return DeliverableAnalysisConfig.builder().withDeliverableUrls(urls).withMilestoneId(milestoneId).build();
    }

    private DeliverableAnalysisConfig productDeliverableAnalysisConfig() {
        String milestoneId = "13";
        List<String> urls = List.of("http://myurl1.com", "http://myurl2.com");
        ErrataConfig errata = ErrataConfig.builder()
                .productName("productName")
                .productVariant("productVariant")
                .productVersion("productVersion")
                .build();

        return DeliverableAnalysisConfig.builder()
                .withDeliverableUrls(urls)
                .withMilestoneId(milestoneId)
                .withErrata(errata)
                .build();
    }

    @Nested
    class BeforeAdjustments {

        @Test
        void shouldGracefullyFailOnNullConfig() {
            ApplicationException ex = assertThrows(ApplicationException.class, () -> {
                validator.validate(null);
            });

            assertEquals("No configuration provided", ex.getMessage());
        }

        @Test
        void shouldNotFailOnValidMinimalConfig() {

            DeliverableAnalysisConfig config = minimalDeliverableAnalysisConfig();
            ValidationResult result = validator.validate(config);
            assertTrue(result.isValid());
            assertTrue(result.getErrors().isEmpty());
        }

        @Test
        void shouldNotFailOnValidProductConfig() {

            DeliverableAnalysisConfig config = productDeliverableAnalysisConfig();
            ValidationResult result = validator.validate(config);
            assertTrue(result.isValid());
            assertTrue(result.getErrors().isEmpty());
        }
    }
}
