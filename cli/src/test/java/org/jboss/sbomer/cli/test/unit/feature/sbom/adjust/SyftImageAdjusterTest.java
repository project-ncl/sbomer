package org.jboss.sbomer.cli.test.unit.feature.sbom.adjust;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

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

        assertEquals(192, bom.getComponents().size());

        Bom adjusted = adjuster.adjust(bom, tmpDir.toPath());

        assertEquals(
                "pkg:oci/console-ui-rhel9@sha256%3Aee4e27734a21cc6b8a8597ef2af32822ad0b4677dbde0a794509f55cbaff5ab3?arch=amd64&os=linux&tag=2.7.0-8.1718294415",
                adjusted.getMetadata().getComponent().getPurl());
        assertEquals("amq-streams/console-ui-rhel9", adjusted.getMetadata().getComponent().getName());
        assertEquals(1, adjusted.getComponents().size());
        assertEquals(32, adjusted.getComponents().get(0).getComponents().size());
        assertEquals(33, adjusted.getDependencies().size());
    }

    @Test
    void removeAllRpmsLeavePrefix() throws IOException {
        SyftImageAdjuster adjuster = new SyftImageAdjuster(List.of("/app"), false);

        assertEquals(192, bom.getComponents().size());

        Bom adjusted = adjuster.adjust(bom, tmpDir.toPath());

        assertEquals(1, adjusted.getComponents().size());
        assertEquals(23, adjusted.getComponents().get(0).getComponents().size());
        assertEquals(24, adjusted.getDependencies().size());
    }

    @Test
    void preserveRpms() throws IOException {
        SyftImageAdjuster adjuster = new SyftImageAdjuster(null, true);

        assertEquals(192, bom.getComponents().size());

        Bom adjusted = adjuster.adjust(bom, tmpDir.toPath());

        assertEquals(1, adjusted.getComponents().size());
        assertEquals(191, adjusted.getComponents().get(0).getComponents().size());
        assertEquals(192, adjusted.getDependencies().size());
    }

    @Test
    void preserveRpmsWithPrefix() throws IOException {
        SyftImageAdjuster adjuster = new SyftImageAdjuster(List.of("/app"), true);

        assertEquals(192, bom.getComponents().size());

        Bom adjusted = adjuster.adjust(bom, tmpDir.toPath());

        assertEquals(1, adjusted.getComponents().size());
        assertEquals(182, adjusted.getComponents().get(0).getComponents().size());
        assertEquals(183, adjusted.getDependencies().size());
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

        // 11th component before adjustment becomes 11th element nested under a root component.
        assertEquals(
                "pkg:rpm/redhat/bzip2-libs@1.0.8-8.el9?arch=x86_64",
                adjusted.getComponents().get(0).getComponents().get(10).getPurl());
        assertEquals(
                "pkg:rpm/redhat/dbus@1.12.20-7.el9_2.1?arch=x86_64&epoch=1",
                adjusted.getComponents().get(0).getComponents().get(21).getPurl());
    }

    @Test
    void shouldAdjustNameAndPurl() {
        SyftImageAdjuster adjuster = new SyftImageAdjuster();

        assertEquals("registry.com/rh-osbs/amq-streams-console-ui-rhel9", bom.getMetadata().getComponent().getName());
        assertNull(bom.getMetadata().getComponent().getPurl());

        Bom adjusted = adjuster.adjust(bom, tmpDir.toPath());

        assertEquals("amq-streams/console-ui-rhel9", adjusted.getMetadata().getComponent().getName());
        assertEquals(
                "pkg:oci/console-ui-rhel9@sha256%3Aee4e27734a21cc6b8a8597ef2af32822ad0b4677dbde0a794509f55cbaff5ab3?arch=amd64&os=linux&tag=2.7.0-8.1718294415",
                adjusted.getMetadata().getComponent().getPurl());
    }

    @Test
    void generatedDependenciesShouldHaveProperDependsOn() throws IOException {
        SyftImageAdjuster adjuster = new SyftImageAdjuster();

        assertEquals(192, bom.getComponents().size());
        // assertEquals("", bom.getMetadata().getComponent().getPurl());

        Bom adjusted = adjuster.adjust(bom, tmpDir.toPath());

        assertEquals(1, bom.getComponents().size());
        // It is one less after filtering, because the "operating system" component does not have a purl.
        assertEquals(191, bom.getComponents().get(0).getComponents().size());
        // Main component + all of it's components
        assertEquals(192, adjusted.getDependencies().size());
        // Main component dependencies
        assertEquals(191, adjusted.getDependencies().get(0).getDependencies().size());
    }
}
