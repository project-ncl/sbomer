package org.jboss.sbomer.cli.test.unit.feature.sbom.adjust;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.util.List;

import org.cyclonedx.model.Bom;
import org.cyclonedx.model.Dependency;
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

    @Test
    void shouldRemoveUnreferencedDependencies() throws IOException {
        // SBOMER-236
        Bom bom = SbomUtils.fromString(TestResources.asString("boms/12E33D1B7C44431.json"));
        String bogusRef = "pkg:maven/org.kie.trustyai/explainability-service@999.0.0.managedsvc-redhat-01048?type=jar";
        boolean found = false;

        // Recursively search for the ref in all dependencies
        for (Dependency dependency : bom.getDependencies()) {
            if (findRefInDependency(dependency, bogusRef)) {
                found = true;
                break;
            }
        }

        assertTrue(found);

        found = false;
        Bom adjusted = adjuster.adjust(bom);
        // Recursively search for the ref in all dependencies
        for (Dependency dependency : adjusted.getDependencies()) {
            if (findRefInDependency(dependency, bogusRef)) {
                found = true;
                break;
            }
        }

        assertFalse(found);
    }

    private static boolean findRefInDependency(Dependency dependency, String refToFind) {
        if (dependency.getRef().equals(refToFind)) {
            return true;
        }

        List<Dependency> childDependencies = dependency.getDependencies();
        if (childDependencies != null) {
            for (Dependency childDependency : childDependencies) {
                if (findRefInDependency(childDependency, refToFind)) {
                    return true;
                }
            }
        }

        return false;
    }
}
