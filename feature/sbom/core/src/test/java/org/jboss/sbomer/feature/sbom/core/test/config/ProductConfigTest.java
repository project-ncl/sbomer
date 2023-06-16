package org.jboss.sbomer.feature.sbom.core.test.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.jboss.sbomer.feature.sbom.core.config.runtime.Config;
import org.jboss.sbomer.feature.sbom.core.config.runtime.GeneratorConfig;
import org.jboss.sbomer.feature.sbom.core.config.runtime.ProductConfig;
import org.jboss.sbomer.feature.sbom.core.enums.GeneratorType;
import org.junit.jupiter.api.Test;

public class ProductConfigTest {

    @Test
    void testGenerateCommand() {
        Config config = Config.builder()
                .buildId("AABBCC")
                .products(
                        List.of(
                                ProductConfig.builder()
                                        .generator(
                                                GeneratorConfig.builder()
                                                        .type(GeneratorType.MAVEN_CYCLONEDX)
                                                        .version("1.2.3")
                                                        .args("--some-switch")
                                                        .build())
                                        .build()))
                .build();

        assertEquals(
                List.of(
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
}
