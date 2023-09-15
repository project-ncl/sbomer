package org.jboss.sbomer.core.test.unit.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
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
import org.jboss.sbomer.core.features.sbom.config.runtime.ProcessorConfig;
import org.jboss.sbomer.core.features.sbom.config.runtime.ProductConfig;
import org.jboss.sbomer.core.features.sbom.config.runtime.RedHatProductProcessorConfig;
import org.jboss.sbomer.core.features.sbom.enums.GeneratorType;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class ConfigSchemaValidatorTest {
    ConfigSchemaValidator validator = new ConfigSchemaValidator();

    private Config minimalRuntimeConfig() {
        List<ProcessorConfig> processors = new ArrayList<>();

        processors.add(
                RedHatProductProcessorConfig.builder()
                        .withErrata(
                                ErrataConfig.builder()
                                        .productName("CCCDDD")
                                        .productVersion("CCDD")
                                        .productVariant("CD")
                                        .build())
                        .build());

        ProductConfig productConfig = ProductConfig.builder()
                .withGenerator(GeneratorConfig.builder().type(GeneratorType.MAVEN_CYCLONEDX).build())
                // .withProcessors(processors)

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
            ValidationResult result = validator.validate(minimalRuntimeConfig());

            assertFalse(result.isValid());

            MatcherAssert.assertThat(
                    result.getErrors(),
                    CoreMatchers.hasItems(
                            "#/products: Property \"products\" does not match schema",
                            "#/products: Items did not match schema",
                            "#/products/0/processors: Property \"processors\" does not match schema",
                            "#/products/0/processors: Array has too few items ( + 0 < 2)",
                            "#/products/0/processors: Property \"processors\" does not match additional properties schema",
                            "#/products: Property \"products\" does not match additional properties schema"));
        }

        @Test
        void shouldFailOnMissingRedHatProductProcessor() {

            Config config = minimalRuntimeConfig();

            config.getProducts().get(0).getProcessors().add(DefaultProcessorConfig.builder().build());

            ValidationResult result = validator.validate(config);

            assertFalse(result.isValid());

            MatcherAssert.assertThat(
                    result.getErrors(),
                    CoreMatchers.hasItems(
                            "#/products: Property \"products\" does not match schema",
                            "#/products: Items did not match schema",
                            "#/products/0/processors: Property \"processors\" does not match schema",
                            "#/products/0/processors: Array has too few items ( + 1 < 2)",
                            "#/products/0/processors: Property \"processors\" does not match additional properties schema",
                            "#/products: Property \"products\" does not match additional properties schema"));
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
