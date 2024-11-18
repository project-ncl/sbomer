package org.jboss.sbomer.cli.test.unit.feature.sbom.adjust;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import org.cyclonedx.model.Bom;
import org.cyclonedx.model.Component;
import org.cyclonedx.model.Dependency;
import org.cyclonedx.model.ExternalReference;
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
        SyftImageAdjuster adjuster = new SyftImageAdjuster(tmpDir.toPath(), null, false);

        assertEquals(192, bom.getComponents().size());
        assertEquals(0, SbomUtils.validate(SbomUtils.toJsonNode(bom)).size());

        Bom adjusted = adjuster.adjust(bom);

        assertEquals(
                "pkg:oci/console-ui-rhel9@sha256%3Aee4e27734a21cc6b8a8597ef2af32822ad0b4677dbde0a794509f55cbaff5ab3?arch=amd64&os=linux&tag=2.7.0-8.1718294415",
                adjusted.getMetadata().getComponent().getPurl());
        assertEquals("amq-streams/console-ui-rhel9", adjusted.getMetadata().getComponent().getName());
        assertEquals(33, adjusted.getComponents().size());
        assertEquals(33, adjusted.getDependencies().size());
        assertEquals(0, SbomUtils.validate(SbomUtils.toJsonNode(adjusted)).size());
    }

    @Test
    void removeAllRpmsLeavePrefix() throws IOException {
        SyftImageAdjuster adjuster = new SyftImageAdjuster(tmpDir.toPath(), List.of("/app"), false);

        assertEquals(192, bom.getComponents().size());
        assertEquals(0, SbomUtils.validate(SbomUtils.toJsonNode(bom)).size());

        Bom adjusted = adjuster.adjust(bom);

        assertEquals(24, adjusted.getComponents().size());
        assertEquals(24, adjusted.getDependencies().size());
        assertEquals(0, SbomUtils.validate(SbomUtils.toJsonNode(adjusted)).size());
    }

    @Test
    void preserveRpms() throws IOException {
        SyftImageAdjuster adjuster = new SyftImageAdjuster(tmpDir.toPath(), null, true);

        assertEquals(192, bom.getComponents().size());
        assertEquals(0, SbomUtils.validate(SbomUtils.toJsonNode(bom)).size());

        Bom adjusted = adjuster.adjust(bom);

        assertEquals(192, adjusted.getComponents().size());
        assertEquals(192, adjusted.getDependencies().size());
        assertEquals(0, SbomUtils.validate(SbomUtils.toJsonNode(adjusted)).size());
    }

    @Test
    void preserveRpmsWithPrefix() throws IOException {
        SyftImageAdjuster adjuster = new SyftImageAdjuster(tmpDir.toPath(), List.of("/app"), true);

        assertEquals(192, bom.getComponents().size());
        assertEquals(0, SbomUtils.validate(SbomUtils.toJsonNode(bom)).size());

        Bom adjusted = adjuster.adjust(bom);

        assertEquals(183, adjusted.getComponents().size());
        assertEquals(183, adjusted.getDependencies().size());
        assertEquals(0, SbomUtils.validate(SbomUtils.toJsonNode(adjusted)).size());
    }

    @Test
    void shouldLeaveOnlyArchAndEpochQualifiers() throws IOException {
        SyftImageAdjuster adjuster = new SyftImageAdjuster(tmpDir.toPath(), null, true);

        assertEquals(
                "pkg:rpm/redhat/bzip2-libs@1.0.8-8.el9?arch=x86_64&upstream=bzip2-1.0.8-8.el9.src.rpm&distro=rhel-9.2",
                bom.getComponents().get(10).getPurl());

        assertEquals(
                "pkg:rpm/redhat/dbus@1.12.20-7.el9_2.1?arch=x86_64&epoch=1&upstream=dbus-1.12.20-7.el9_2.1.src.rpm&distro=rhel-9.2",
                bom.getComponents().get(21).getPurl());

        assertEquals(0, SbomUtils.validate(SbomUtils.toJsonNode(bom)).size());

        Bom adjusted = adjuster.adjust(bom);

        // 11th component before adjustment becomes 12th after adding main component
        assertEquals("pkg:rpm/redhat/bzip2-libs@1.0.8-8.el9?arch=x86_64", adjusted.getComponents().get(11).getPurl());
        assertEquals(
                "pkg:rpm/redhat/dbus@1.12.20-7.el9_2.1?arch=x86_64&epoch=1",
                adjusted.getComponents().get(22).getPurl());
        assertEquals(0, SbomUtils.validate(SbomUtils.toJsonNode(adjusted)).size());
    }

    @Test
    void shouldAdjustNameAndPurl() throws IOException {
        SyftImageAdjuster adjuster = new SyftImageAdjuster(tmpDir.toPath());

        assertEquals("registry.com/rh-osbs/amq-streams-console-ui-rhel9", bom.getMetadata().getComponent().getName());
        assertNull(bom.getMetadata().getComponent().getPurl());
        assertEquals(0, SbomUtils.validate(SbomUtils.toJsonNode(bom)).size());

        Bom adjusted = adjuster.adjust(bom);

        assertEquals("amq-streams/console-ui-rhel9", adjusted.getMetadata().getComponent().getName());
        assertEquals(
                "pkg:oci/console-ui-rhel9@sha256%3Aee4e27734a21cc6b8a8597ef2af32822ad0b4677dbde0a794509f55cbaff5ab3?arch=amd64&os=linux&tag=2.7.0-8.1718294415",
                adjusted.getMetadata().getComponent().getPurl());
        assertEquals(0, SbomUtils.validate(SbomUtils.toJsonNode(adjusted)).size());
    }

    @Test
    void generatedDependenciesShouldHaveProperDependsOn() throws IOException {
        SyftImageAdjuster adjuster = new SyftImageAdjuster(tmpDir.toPath());

        assertEquals(192, bom.getComponents().size());
        assertEquals(0, SbomUtils.validate(SbomUtils.toJsonNode(bom)).size());

        Bom adjusted = adjuster.adjust(bom);

        assertEquals(192, bom.getComponents().size());
        // Main component + all of it's components
        assertEquals(192, adjusted.getDependencies().size());
        // Main component dependencies
        assertEquals(191, adjusted.getDependencies().get(0).getDependencies().size());
        assertEquals(0, SbomUtils.validate(SbomUtils.toJsonNode(adjusted)).size());
    }

    @Test
    void depdsShouldPointToComponents() throws Exception {
        SyftImageAdjuster adjuster = new SyftImageAdjuster(tmpDir.toPath());

        this.bom = SbomUtils.fromString(TestResources.asString("boms/shaded.json"));

        assertEquals(459, bom.getComponents().size());
        assertEquals(294, bom.getDependencies().size());
        assertEquals(0, SbomUtils.validate(SbomUtils.toJsonNode(bom)).size());

        Bom adjusted = adjuster.adjust(bom);

        // One component added (image) at position 0, one component removed (operating system), size is the same as
        // before
        assertEquals(459, adjusted.getComponents().size());
        assertEquals(459, adjusted.getDependencies().size());
        assertEquals(0, SbomUtils.validate(SbomUtils.toJsonNode(adjusted)).size());

        for (Dependency d : bom.getDependencies()) {
            if (bom.getComponents().stream().filter(c -> c.getBomRef().equals(d.getRef())).findAny().isEmpty()) {
                throw new Exception("The dependency " + d.getRef() + " does not have a related component!");
            }
        }
    }

    // https://issues.redhat.com/browse/SBOMER-197
    @Test
    void metadataComponentShouldBeBareRepresentationOfMainComponent() throws IOException {
        SyftImageAdjuster adjuster = new SyftImageAdjuster(tmpDir.toPath(), null, true);

        assertFalse(bom.getMetadata().getProperties().isEmpty());

        Bom adjusted = adjuster.adjust(bom);

        Component metadataComponent = adjusted.getMetadata().getComponent();

        // Has purl
        assertEquals(
                "pkg:oci/console-ui-rhel9@sha256%3Aee4e27734a21cc6b8a8597ef2af32822ad0b4677dbde0a794509f55cbaff5ab3?arch=amd64&os=linux&tag=2.7.0-8.1718294415",
                metadataComponent.getPurl());
        // Has type
        assertEquals(Component.Type.CONTAINER, metadataComponent.getType());
        // Has name
        assertEquals("amq-streams/console-ui-rhel9", metadataComponent.getName());
        // No properties
        assertNull(metadataComponent.getProperties());

        // Main component is the first one
        Component mainComponent = adjusted.getComponents().get(0);

        assertFalse(mainComponent.getProperties().isEmpty());
        // Has purl
        assertEquals(
                "pkg:oci/console-ui-rhel9@sha256%3Aee4e27734a21cc6b8a8597ef2af32822ad0b4677dbde0a794509f55cbaff5ab3?arch=amd64&os=linux&tag=2.7.0-8.1718294415",
                mainComponent.getPurl());
        // Has type
        assertEquals(Component.Type.CONTAINER, mainComponent.getType());
        // Has name
        assertEquals("amq-streams/console-ui-rhel9", mainComponent.getName());
        // Other things, for example bom-ref
        assertEquals("3893910a10b83660", mainComponent.getBomRef());

        // Is valid
        assertEquals(0, SbomUtils.validate(SbomUtils.toJsonNode(adjusted)).size());
    }

    @Test
    void removeExternalReferencesWithBadURI() throws IOException {
        SyftImageAdjuster adjuster = new SyftImageAdjuster(tmpDir.toPath(), null, false);
        final String badUrl = "https://github.com:facebook/regenerator/tree/master/packages/regenerator-runtime";

        assertEquals(24, getExternalReferenceStream(bom).count());
        assertTrue(getExternalReferenceStream(bom).anyMatch(r -> badUrl.endsWith(r.getUrl())));
        assertEquals(0, SbomUtils.validate(SbomUtils.toJsonNode(bom)).size());

        Bom adjusted = adjuster.adjust(bom);

        assertEquals(21, getExternalReferenceStream(adjusted).count());
        assertFalse(getExternalReferenceStream(adjusted).anyMatch(r -> badUrl.endsWith(r.getUrl())));
        assertEquals(0, SbomUtils.validate(SbomUtils.toJsonNode(adjusted)).size());
    }

    private Stream<ExternalReference> getExternalReferenceStream(Bom bom) {
        return bom.getComponents()
            .stream()
            .map(Component::getExternalReferences)
            .filter(Objects::nonNull)
            .flatMap(Collection::stream);
    }
}
