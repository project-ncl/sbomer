package org.jboss.sbomer.core.test.unit.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.jboss.sbomer.core.config.DefaultGenerationConfig;
import org.jboss.sbomer.core.config.SbomerConfigProvider;
import org.jboss.sbomer.core.features.sbom.config.runtime.Config;
import org.jboss.sbomer.core.features.sbom.config.runtime.DefaultProcessorConfig;
import org.jboss.sbomer.core.features.sbom.config.runtime.ErrataConfig;
import org.jboss.sbomer.core.features.sbom.config.runtime.GeneratorConfig;
import org.jboss.sbomer.core.features.sbom.config.runtime.OperationConfig;
import org.jboss.sbomer.core.features.sbom.config.runtime.ProcessorConfig;
import org.jboss.sbomer.core.features.sbom.config.runtime.ProductConfig;
import org.jboss.sbomer.core.features.sbom.config.runtime.RedHatProductProcessorConfig;
import org.jboss.sbomer.core.features.sbom.enums.GeneratorType;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class SbomerConfigProviderTest {

    /**
     * Ensure default settings for generation.
     */
    @Test
    void shouldReturnDefaultGenerationConfig() {
        DefaultGenerationConfig defaultGenerationConfig = SbomerConfigProvider.getInstance()
                .getDefaultGenerationConfig();

        assertNotNull(defaultGenerationConfig);

        assertTrue(defaultGenerationConfig.isEnabled());
        assertEquals(GeneratorType.MAVEN_CYCLONEDX, defaultGenerationConfig.defaultGenerator());
        assertEquals(5, defaultGenerationConfig.generators().size());
        assertEquals(
                "--batch-mode",
                defaultGenerationConfig.generators().get(GeneratorType.MAVEN_CYCLONEDX).defaultArgs());
        assertEquals(
                "--warn-on-missing-scm --legacy-scm-locator",
                defaultGenerationConfig.generators().get(GeneratorType.MAVEN_DOMINO).defaultArgs());
        assertEquals("-info", defaultGenerationConfig.generators().get(GeneratorType.GRADLE_CYCLONEDX).defaultArgs());
    }

    @Nested
    class ConfigAdjusting {

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
                    .withProcessors(processors)

                    .build();

            return Config.builder().withBuildId("AABBCC").withProducts(List.of(productConfig)).build();
        }

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
                    .withProcessors(processors)

                    .build();

            return OperationConfig.builder().withOperationId("OPERATIONAABBCC").withProduct(productConfig).build();
        }

        @Test
        void shouldNotFailOnEmptyProducts() {
            Config config = Config.builder().withBuildId("ABCDEFG").build();
            SbomerConfigProvider.getInstance().adjust(config);
        }

        @Test
        void shouldProvideDefaultValues() {
            Config config = minimalRuntimeConfig();
            SbomerConfigProvider.getInstance().adjust(config);

            ProductConfig product = config.getProducts().get(0);

            assertEquals(2, product.getProcessors().size());
            assertTrue(product.getProcessors().get(0) instanceof DefaultProcessorConfig);
            assertTrue(product.getProcessors().get(1) instanceof RedHatProductProcessorConfig);
            assertEquals(GeneratorType.MAVEN_CYCLONEDX, product.getGenerator().getType());
            assertEquals("--batch-mode", product.getGenerator().getArgs());
            assertEquals("2.7.9", product.getGenerator().getVersion());
        }

        @Test
        void shouldNotAdjustExplicitGeneratorVersion() {
            Config config = minimalRuntimeConfig();

            config.getProducts().get(0).getGenerator().setVersion("1.1.1");

            SbomerConfigProvider.getInstance().adjust(config);
            ProductConfig product = config.getProducts().get(0);

            assertEquals(2, product.getProcessors().size());
            assertTrue(product.getProcessors().get(0) instanceof DefaultProcessorConfig);
            assertTrue(product.getProcessors().get(1) instanceof RedHatProductProcessorConfig);
            assertEquals(GeneratorType.MAVEN_CYCLONEDX, product.getGenerator().getType());
            assertEquals("--batch-mode", product.getGenerator().getArgs());
            assertEquals("1.1.1", product.getGenerator().getVersion());
        }

        @Test
        void shouldNotAdjustExplicitGeneratorArgs() {
            Config config = minimalRuntimeConfig();

            config.getProducts().get(0).getGenerator().setArgs("--custom-args");

            SbomerConfigProvider.getInstance().adjust(config);
            ProductConfig product = config.getProducts().get(0);

            assertEquals(2, product.getProcessors().size());
            assertTrue(product.getProcessors().get(0) instanceof DefaultProcessorConfig);
            assertTrue(product.getProcessors().get(1) instanceof RedHatProductProcessorConfig);
            assertEquals(GeneratorType.MAVEN_CYCLONEDX, product.getGenerator().getType());
            assertEquals("--custom-args", product.getGenerator().getArgs());
            assertEquals("2.7.9", product.getGenerator().getVersion());
        }

        @Test
        void shouldNotAddTwiceDefaultProcessor() {
            Config config = minimalRuntimeConfig();

            config.getProducts().get(0).getProcessors().add(0, DefaultProcessorConfig.builder().build());

            SbomerConfigProvider.getInstance().adjust(config);
            ProductConfig product = config.getProducts().get(0);

            assertEquals(2, product.getProcessors().size());
            assertTrue(product.getProcessors().get(0) instanceof DefaultProcessorConfig);
            assertTrue(product.getProcessors().get(1) instanceof RedHatProductProcessorConfig);
        }

        @Test
        void shouldNotAdjustExplicitGeneratorOperationArgs() {
            OperationConfig config = minimalRuntimeOperationConfig();

            config.getProduct().getGenerator().setArgs("--custom-args");

            SbomerConfigProvider.getInstance().adjust(config);
            ProductConfig product = config.getProduct();

            assertEquals(1, product.getProcessors().size());
            assertEquals(GeneratorType.CYCLONEDX_OPERATION, product.getGenerator().getType());
            assertNull(product.getGenerator().getArgs());
            assertNull(product.getGenerator().getVersion());
        }
    }
}
