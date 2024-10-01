package org.jboss.sbomer.core.test.unit.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import org.jboss.sbomer.core.features.sbom.config.Config;
import org.jboss.sbomer.core.features.sbom.config.DeliverableAnalysisConfig;
import org.jboss.sbomer.core.features.sbom.config.OperationConfig;
import org.jboss.sbomer.core.features.sbom.config.PncBuildConfig;
import org.jboss.sbomer.core.features.sbom.config.SyftImageConfig;
import org.jboss.sbomer.core.test.TestResources;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class ConfigTest {
    @Test
    void shouldConvertFromStringNotFailOnNullValue() {
        assertNull(Config.fromString(null));
    }

    @Test
    void shouldConvertFromStringNotFailOnEmptyString() {
        assertNull(Config.fromString(""));
    }

    /**
     * By default we assume that it is a PncBuildConfig object for backwards-compatibility.
     */
    @Test
    void shouldConvertFromStringEmptyObject() {
        assertEquals(new PncBuildConfig(), Config.fromString("{}"));
    }

    @Test
    void shouldConvertToSyftImageConfig() {
        assertEquals(new SyftImageConfig(), Config.fromString("{\"type\": \"syft-image\"}"));
    }

    @Test
    void shouldConvertToOperationConfig() {
        assertEquals(new OperationConfig(), Config.fromString("{\"type\": \"operation\"}"));
    }

    @Test
    void shouldConvertToAnalysisConfig() {
        assertEquals(new DeliverableAnalysisConfig(), Config.fromString("{\"type\": \"analysis\"}"));
    }

    @Test
    void shouldConvertToPncBuildConfig() {
        assertEquals(new PncBuildConfig(), Config.fromString("{\"type\": \"pnc-build\"}"));
    }

    @Nested
    class DeliverableAnalysisConfigTest {

        @Test
        void shouldReturnDefault() {
            DeliverableAnalysisConfig config = Config.newInstance(DeliverableAnalysisConfig.class);

            assertEquals("sbomer.jboss.org/v1alpha1", config.getApiVersion());
            assertNull(config.getDeliverableUrls());
            assertNull(config.getErrata());
            assertNull(config.getMilestoneId());
        }
    }

    @Nested
    class OperationConfigTest {

        @Test
        void shouldReturnDefault() {
            OperationConfig config = Config.newInstance(OperationConfig.class);

            assertEquals("sbomer.jboss.org/v1alpha1", config.getApiVersion());
            assertNull(config.getDeliverableUrls());
            assertNull(config.getOperationId());
            assertNull(config.getProduct());
        }
    }

    @Nested
    class PncBuildConfigTest {

        @Test
        void shouldReturnDefault() {
            PncBuildConfig config = Config.newInstance(PncBuildConfig.class);

            assertEquals("sbomer.jboss.org/v1alpha1", config.getApiVersion());
            assertNull(config.getBuildId());
            assertNull(config.getEnvironment());
            assertTrue(config.getProducts().isEmpty());
        }
    }

    @Nested
    class SyftImageConfigTest {

        @Test
        void shouldReturnNewInstance() {
            SyftImageConfig config = Config.newInstance(SyftImageConfig.class);

            assertEquals("sbomer.jboss.org/v1alpha1", config.getApiVersion());
            assertTrue(config.getPaths().isEmpty());
            assertFalse(config.isIncludeRpms());

        }

        @Test
        void shouldAddDefaultProcessor() {
            assertEquals("default", new SyftImageConfig().toProcessorsCommand());
        }

        @Test
        void shouldReturnValidProcessorCommand() throws IOException {
            assertEquals(
                    "default",
                    Config.fromString(TestResources.asString("configs/syft-image-plain.json")).toProcessorsCommand());
        }

        @Test
        void shouldReturnValidProcessorCommandForRhProduct() throws IOException {
            assertEquals(
                    "default redhat-product --productName pName --productVersion pVersion --productVariant pVariant",
                    Config.fromString(TestResources.asString("configs/syft-image-product.json")).toProcessorsCommand());
        }
    }
}
