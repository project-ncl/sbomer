package org.jboss.sbomer.core.test.unit.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.jboss.sbomer.core.features.sbom.config.Config;
import org.jboss.sbomer.core.features.sbom.config.DeliverableAnalysisConfig;
import org.jboss.sbomer.core.features.sbom.config.OperationConfig;
import org.jboss.sbomer.core.features.sbom.config.PncBuildConfig;
import org.jboss.sbomer.core.features.sbom.config.SyftImageConfig;
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
}
