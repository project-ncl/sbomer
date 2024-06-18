package org.jboss.sbomer.core.test.unit.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.jboss.sbomer.core.SchemaValidator.ValidationResult;
import org.jboss.sbomer.core.config.OperationConfigSchemaValidator;
import org.jboss.sbomer.core.errors.ApplicationException;
import org.jboss.sbomer.core.features.sbom.config.runtime.ErrataConfig;
import org.jboss.sbomer.core.features.sbom.config.runtime.GeneratorConfig;
import org.jboss.sbomer.core.features.sbom.config.runtime.OperationConfig;
import org.jboss.sbomer.core.features.sbom.config.runtime.ProcessorConfig;
import org.jboss.sbomer.core.features.sbom.config.runtime.ProductConfig;
import org.jboss.sbomer.core.features.sbom.config.runtime.RedHatProductProcessorConfig;
import org.jboss.sbomer.core.features.sbom.enums.GeneratorType;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class OperationConfigSchemaValidatorTest {
    OperationConfigSchemaValidator validator = new OperationConfigSchemaValidator();

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
        void shouldNotFailOnValidConfig() {

            OperationConfig config = minimalRuntimeOperationConfig();
            ValidationResult result = validator.validate(config);

            assertTrue(result.isValid());
            assertTrue(result.getErrors().isEmpty());
        }
    }
}
