package org.jboss.sbomer.core.test.unit.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.jboss.sbomer.core.SchemaValidator.ValidationResult;
import org.jboss.sbomer.core.config.ConfigSchemaValidator;
import org.jboss.sbomer.core.errors.ApplicationException;
import org.jboss.sbomer.core.features.sbom.config.runtime.Config;
import org.jboss.sbomer.core.features.sbom.config.runtime.DefaultProcessorConfig;
import org.jboss.sbomer.core.features.sbom.config.runtime.ErrataConfig;
import org.jboss.sbomer.core.features.sbom.config.runtime.GeneratorConfig;
import org.jboss.sbomer.core.features.sbom.config.runtime.ProductConfig;
import org.jboss.sbomer.core.features.sbom.config.runtime.RedHatProductProcessorConfig;
import org.jboss.sbomer.core.features.sbom.enums.GeneratorType;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class ConfigSchemaValidatorTest {
    ConfigSchemaValidator validator = new ConfigSchemaValidator();

    private Config minimalRuntimeConfig() {
        ProductConfig productConfig = ProductConfig.builder()
                .withGenerator(GeneratorConfig.builder().type(GeneratorType.MAVEN_CYCLONEDX).build())

                .build();

        return Config.builder().withBuildId("AABBCC").withProducts(List.of(productConfig)).build();
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
        void shouldFailOnInvalidObjectListingAllProblems() {
            Config config = minimalRuntimeConfig();
            // Make the generator type not set
            config.getProducts().get(0).getGenerator().setType(null);

            ValidationResult result = validator.validate(config);

            assertFalse(result.isValid());

            MatcherAssert.assertThat(
                    result.getErrors(),
                    CoreMatchers.hasItems(
                            "#/products: Property \"products\" does not match schema",
                            "#/products: Items did not match schema",
                            "#/products/0/generator: Property \"generator\" does not match schema",
                            "#/products/0/generator: Instance does not have required property \"type\"",
                            "#/products/0/generator: Property \"generator\" does not match additional properties schema",
                            "#/products: Property \"products\" does not match additional properties schema"));
        }

        /**
         * With the feautre to generate minifests for all builds (https://issues.redhat.com/browse/SBOMER-14) the Red
         * Hat Product processor has been made optional.
         */
        @Test
        void shouldNotOnMissingRedHatProductProcessor() {

            Config config = minimalRuntimeConfig();

            config.getProducts().get(0).getProcessors().add(DefaultProcessorConfig.builder().build());

            ValidationResult result = validator.validate(config);

            assertTrue(result.isValid());
        }

        @Test
        void shouldNotFailOnValidConfig() {

            Config config = minimalRuntimeConfig();

            config.getProducts().get(0).getProcessors().add(DefaultProcessorConfig.builder().build());
            config.getProducts()
                    .get(0)
                    .getProcessors()
                    .add(
                            RedHatProductProcessorConfig.builder()
                                    .withErrata(
                                            ErrataConfig.builder()
                                                    .productName("PN")
                                                    .productVersion("PV")
                                                    .productVariant("PVAR")
                                                    .build())
                                    .build());

            ValidationResult result = validator.validate(config);

            assertTrue(result.isValid());
            assertTrue(result.getErrors().isEmpty());
        }
    }
}
