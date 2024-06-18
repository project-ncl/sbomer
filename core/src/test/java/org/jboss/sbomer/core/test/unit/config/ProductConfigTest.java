package org.jboss.sbomer.core.test.unit.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.jboss.sbomer.core.features.sbom.config.runtime.Config;
import org.jboss.sbomer.core.features.sbom.config.runtime.GeneratorConfig;
import org.jboss.sbomer.core.features.sbom.config.runtime.OperationConfig;
import org.jboss.sbomer.core.features.sbom.config.runtime.ProductConfig;
import org.jboss.sbomer.core.features.sbom.enums.GeneratorType;
import org.junit.jupiter.api.Test;

class ProductConfigTest {

    @Test
    void testGenerateCommand() {
        Config config = Config.builder()
                .withBuildId("AABBCC")
                .withProducts(
                        List.of(
                                ProductConfig.builder()
                                        .withGenerator(
                                                GeneratorConfig.builder()
                                                        .type(GeneratorType.MAVEN_CYCLONEDX)
                                                        .version("1.2.3")
                                                        .args("--some-switch")
                                                        .build())
                                        .build()))
                .build();

        assertEquals(
                List.of(
                        "-v",
                        "sbom",
                        "generate",
                        "--build-id",
                        "AABBCC",
                        "maven-cyclonedx",
                        "--tool-version",
                        "1.2.3",
                        "--tool-args",
                        "--some-switch"),
                config.getProducts().get(0).generateCommand(config));
    }

    @Test
    void testGenerateOperationCommand() {
        OperationConfig config = OperationConfig.builder()
                .withOperationId("AABBCCDDD")
                .withProduct(

                        ProductConfig.builder()
                                .withGenerator(
                                        GeneratorConfig.builder().type(GeneratorType.CYCLONEDX_OPERATION).build())
                                .build())
                .build();

        assertEquals(
                List.of("-v", "sbom", "generate-operation", "--operation-id", "AABBCCDDD", "cyclonedx-operation"),
                config.getProduct().generateCommand(config));
    }
}
