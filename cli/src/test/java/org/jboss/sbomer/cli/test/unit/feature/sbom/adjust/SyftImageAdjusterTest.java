package org.jboss.sbomer.cli.test.unit.feature.sbom.adjust;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import org.cyclonedx.model.Bom;
import org.cyclonedx.model.Component;
import org.cyclonedx.model.Component.Type;
import org.cyclonedx.model.Dependency;
import org.cyclonedx.model.ExternalReference;
import org.cyclonedx.model.Property;
import org.jboss.sbomer.cli.feature.sbom.adjuster.SyftImageAdjuster;
import org.jboss.sbomer.core.features.sbom.utils.PurlSanitizer;
import org.jboss.sbomer.core.features.sbom.utils.SbomUtils;
import org.jboss.sbomer.core.test.TestResources;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.github.packageurl.MalformedPackageURLException;
import com.github.packageurl.PackageURL;

class SyftImageAdjusterTest {

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

    @Test
    void shouldAdjustVendorAndPublisher() throws IOException {
        SyftImageAdjuster adjuster = new SyftImageAdjuster(tmpDir.toPath());

        Optional<Property> bogusVendor = bom.getMetadata()
                .getProperties()
                .stream()
                .filter(property -> "syft:image:labels:vendor".equals(property.getName()))
                .findFirst();
        assertTrue(bogusVendor.isPresent());
        assertEquals("Red Hat, Inc.", bogusVendor.get().getValue());

        Optional<Component> bogusComponent = bom.getComponents()
                .stream()
                .filter(
                        component -> "pkg:rpm/redhat/alternatives@1.20-2.el9?arch=x86_64&upstream=chkconfig-1.20-2.el9.src.rpm&distro=rhel-9.2"
                                .equals(component.getPurl()))
                .findFirst();
        assertTrue(bogusComponent.isPresent());
        assertEquals("Red Hat, Inc.", bogusComponent.get().getPublisher());

        Bom adjusted = adjuster.adjust(bom);

        Optional<Property> oldVendor = adjusted.getComponents()
                .get(0)
                .getProperties()
                .stream()
                .filter(property -> "syft:image:labels:vendor".equals(property.getName()))
                .findFirst();
        assertFalse(oldVendor.isPresent());

        Optional<Property> goodVendor = adjusted.getComponents()
                .get(0)
                .getProperties()
                .stream()
                .filter(property -> "sbomer:image:labels:vendor".equals(property.getName()))
                .findFirst();
        assertTrue(bogusVendor.isPresent());
        assertEquals("Red Hat", goodVendor.get().getValue());

        Optional<Component> goodComponent = adjusted.getComponents()
                .stream()
                .filter(component -> "Red Hat, Inc.".equals(component.getPublisher()))
                .findFirst();
        assertFalse(goodComponent.isPresent());
    }

    private Stream<ExternalReference> getExternalReferenceStream(Bom bom) {
        return bom.getComponents()
                .stream()
                .map(Component::getExternalReferences)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream);
    }

    @Test
    void shouldSanitizeBogusPurls() throws IOException {
        String bogusPurl = "pkg:rpm/redhat/passt@0^20230222.g4ddbcb9-4.el9_2?arch=x86_64&distro=rhel-9.2&upstream=passt-0^20230222.g4ddbcb9-4.el9_2.src.rpm";

        try {
            new PackageURL(bogusPurl);
            fail("Should fail the parsing to PURL of " + bogusPurl);
        } catch (MalformedPackageURLException e) {
        }

        String sanitizedPurl = PurlSanitizer.sanitizePurl(bogusPurl);

        try {
            new PackageURL(sanitizedPurl);
        } catch (MalformedPackageURLException e) {
            fail("Failed parsing to PURL of sanitized " + sanitizedPurl);
        }

        assertEquals(
                "pkg:rpm/redhat/passt@0-20230222.g4ddbcb9-4.el9_2?arch=x86_64&distro=rhel-9.2&upstream=passt-0-20230222.g4ddbcb9-4.el9_2.src.rpm",
                sanitizedPurl);
    }

    @Test
    void shouldRebuildBogusPurls() throws IOException {
        // Initialize the bogus component generated from Syft and verify it's not fixable, and remains unchanged
        Component component = SbomUtils
                .createComponent(null, "../", "(devel)", null, "pkg:golang/../@(devel)", Component.Type.LIBRARY);
        component.setBomRef("a02ebe2f06983d18");
        SbomUtils.addPropertyIfMissing(component, "syft:package:type", "go-module");

        // Verify that the purl is not valid
        boolean isValid = SbomUtils.isValidPurl(component.getPurl());
        assertFalse(isValid);

        // Verify that the purl cannot be sanitized
        String sanitizedPurl = SbomUtils.sanitizePurl(component.getPurl());
        assertNull(sanitizedPurl);

        // Verify that the purl can be rebuilt
        boolean isValidAfterRebuilding = SbomUtils.hasValidOrSanitizablePurl(component);
        assertTrue(isValidAfterRebuilding);
        assertTrue(SbomUtils.isValidPurl(component.getPurl()));
    }
}
