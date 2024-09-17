package org.jboss.sbomer.cli.test.unit.feature.sbom.adjust;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.cyclonedx.model.Bom;
import org.jboss.sbomer.cli.feature.sbom.adjuster.SyftImageAdjuster;
import org.jboss.sbomer.core.features.sbom.utils.SbomUtils;
import org.jboss.sbomer.core.test.TestResources;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class SyftImageAdjusterTest {

    @TempDir
    File tmpDir;

    Bom bom = null;

    @BeforeEach
    void init() throws IOException {
        Files.writeString(Path.of(tmpDir.getAbsolutePath(), "skopeo.json"), TestResources.asString("skopeo.json"));
        this.bom = SbomUtils.fromString(TestResources.asString("boms/image.json"));
    }

    @Test
    void removeAllRpms() throws IOException {
        SyftImageAdjuster adjuster = new SyftImageAdjuster(null, false);

        Bom adjusted = adjuster.adjust(bom, tmpDir.toPath());

        assertEquals(
                "pkg:oci/amq-streams-console-ui-rhel9@sha256%3Af63b27a29c032843941b15567ebd1f37f540160e8066ac74c05367134c2ff3aa?repository_url=registry.com&os=linux&arch=amd64&tag=2.7.0-8.1718294415",
                adjusted.getMetadata().getComponent().getPurl());
        assertEquals(32, adjusted.getComponents().size());
    }

    @Test
    void removeAllRpmsLeavePrefix() throws IOException {
        SyftImageAdjuster adjuster = new SyftImageAdjuster(List.of("/app"), false);

        Bom adjusted = adjuster.adjust(bom, tmpDir.toPath());

        assertEquals(23, adjusted.getComponents().size());
    }

    @Test
    void preserveRpms() throws IOException {
        SyftImageAdjuster adjuster = new SyftImageAdjuster(null, true);

        Bom adjusted = adjuster.adjust(bom, tmpDir.toPath());

        assertEquals(191, adjusted.getComponents().size());
    }

    @Test
    void preserveRpmsWithPrefix() throws IOException {
        SyftImageAdjuster adjuster = new SyftImageAdjuster(List.of("/app"), true);

        Bom adjusted = adjuster.adjust(bom, tmpDir.toPath());

        assertEquals(182, adjusted.getComponents().size());
    }

    @Test
    void shouldLeaveOnlyArchAndEpochQualifiers() throws IOException {
        SyftImageAdjuster adjuster = new SyftImageAdjuster(null, true);

        assertEquals(
                "pkg:rpm/redhat/bzip2-libs@1.0.8-8.el9?arch=x86_64&upstream=bzip2-1.0.8-8.el9.src.rpm&distro=rhel-9.2",
                bom.getComponents().get(10).getPurl());

        assertEquals(
                "pkg:rpm/redhat/dbus@1.12.20-7.el9_2.1?arch=x86_64&epoch=1&upstream=dbus-1.12.20-7.el9_2.1.src.rpm&distro=rhel-9.2",
                bom.getComponents().get(21).getPurl());

        Bom adjusted = adjuster.adjust(bom, tmpDir.toPath());

        assertEquals("pkg:rpm/redhat/bzip2-libs@1.0.8-8.el9?arch=x86_64", adjusted.getComponents().get(10).getPurl());
        assertEquals(
                "pkg:rpm/redhat/dbus@1.12.20-7.el9_2.1?arch=x86_64&epoch=1",
                adjusted.getComponents().get(21).getPurl());
    }

}
