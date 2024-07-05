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
import org.jboss.sbomer.core.features.sbom.config.DeliverableAnalysisConfig;
import org.jboss.sbomer.core.features.sbom.config.OperationConfig;
import org.jboss.sbomer.core.features.sbom.config.PncBuildConfig;
import org.jboss.sbomer.core.features.sbom.config.SyftImageConfig;
import org.jboss.sbomer.core.features.sbom.config.runtime.DefaultProcessorConfig;
import org.jboss.sbomer.core.features.sbom.config.runtime.ErrataConfig;
import org.jboss.sbomer.core.features.sbom.config.runtime.GeneratorConfig;
import org.jboss.sbomer.core.features.sbom.config.runtime.ProcessorConfig;
import org.jboss.sbomer.core.features.sbom.config.runtime.ProductConfig;
import org.jboss.sbomer.core.features.sbom.config.runtime.RedHatProductProcessorConfig;
import org.jboss.sbomer.core.features.sbom.enums.GeneratorType;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ConfigSchemaValidatorTest {
    ConfigSchemaValidator validator = new ConfigSchemaValidator();

    @Nested
    class PncBuildConfigTests {

        private PncBuildConfig minimalRuntimeConfig() {
            ProductConfig productConfig = ProductConfig.builder()
                    .withGenerator(GeneratorConfig.builder().type(GeneratorType.MAVEN_CYCLONEDX).build())

                    .build();

            return PncBuildConfig.builder().withBuildId("AABBCC").withProducts(List.of(productConfig)).build();
        }

        @Test
        void shouldGracefullyFailOnNullConfig() {
            ApplicationException ex = assertThrows(ApplicationException.class, () -> {
                validator.validate(null);
            });

            assertEquals("No configuration provided", ex.getMessage());
        }

        @Test
        void shouldFailOnInvalidObjectListingAllProblems() {
            PncBuildConfig config = minimalRuntimeConfig();
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

            PncBuildConfig config = minimalRuntimeConfig();

            config.getProducts().get(0).getProcessors().add(DefaultProcessorConfig.builder().build());

            ValidationResult result = validator.validate(config);

            assertTrue(result.isValid());
        }

        @Test
        void shouldNotFailOnValidConfig() {

            PncBuildConfig config = minimalRuntimeConfig();

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

    @Nested
    class OperationConfigTests {

        private OperationConfig minimalRuntimeOperationConfig() {
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
                    .withGenerator(GeneratorConfig.builder().type(GeneratorType.CYCLONEDX_OPERATION).build())
                    .build();

            return OperationConfig.builder().withOperationId("OPERATIONAABBCC").withProduct(productConfig).build();
        }

        @Test
        void shouldGracefullyFailOnNullConfig() {
            ApplicationException ex = assertThrows(ApplicationException.class, () -> {
                validator.validate(null);
            });

            assertEquals("No configuration provided", ex.getMessage());
        }

        @Test
        void shouldNotFailOnValidConfig() {

            OperationConfig config = minimalRuntimeOperationConfig();
            ValidationResult result = validator.validate(config);

            assertTrue(result.isValid());
            assertTrue(result.getErrors().isEmpty());
        }
    }

    @Nested
    class DelAnConfigTests {

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

    @Nested
    class SyftConfigTests {
        private SyftImageConfig minimalConfig() {
            return SyftImageConfig.builder().build();
        }

        @Test
        void minimalConfigShouldBeValid() {
            ValidationResult result = validator.validate(minimalConfig());
            assertTrue(result.isValid());
            assertTrue(result.getErrors().isEmpty());
        }

        @Test
        void shouldAllowDirectories() {
            SyftImageConfig config = minimalConfig();

            config.setPaths(List.of("/var", "/opt"));

            ValidationResult result = validator.validate(config);
            assertTrue(result.isValid());
            assertTrue(result.getErrors().isEmpty());
        }
    }
}
