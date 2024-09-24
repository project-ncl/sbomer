package org.jboss.sbomer.cli.test.unit.feature.sbom.adjust;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import org.cyclonedx.model.Bom;
import org.jboss.sbomer.cli.feature.sbom.adjuster.PncBuildAdjuster;
import org.jboss.sbomer.core.features.sbom.utils.SbomUtils;
import org.jboss.sbomer.core.test.TestResources;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class PncBuildAdjusterTest {

    PncBuildAdjuster adjuster = new PncBuildAdjuster();

    Bom bom = null;

    @BeforeEach
    void init() throws IOException {
        this.bom = SbomUtils.fromString(TestResources.asString("boms/image.json"));
    }

    @Test
    void shouldAddSerialNumber() {
        bom.setSerialNumber(null);

        Bom adjusted = adjuster.adjust(bom);

        assertNotNull(adjusted.getSerialNumber());
        assertTrue(adjusted.getSerialNumber().startsWith("urn:uuid:"));
        assertEquals(45, adjusted.getSerialNumber().length());
    }
}
