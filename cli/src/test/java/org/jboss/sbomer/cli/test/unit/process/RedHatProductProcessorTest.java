package org.jboss.sbomer.cli.test.unit.process;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.cyclonedx.model.Bom;
import org.cyclonedx.model.Component;
import org.cyclonedx.model.Metadata;
import org.cyclonedx.model.Property;
import org.jboss.sbomer.cli.feature.sbom.processor.RedHatProductProcessor;
import org.jboss.sbomer.core.features.sbom.utils.SbomUtils;
import org.junit.jupiter.api.Test;

class RedHatProductProcessorTest {
    final RedHatProductProcessor processor = new RedHatProductProcessor("pName", "pVersion", "pVariant");

    @Test
    void addAll() {
        Bom bom = new Bom();
        Component component = new Component();

        Metadata metadata = new Metadata();
        metadata.setComponent(component);

        bom.setMetadata(metadata);

        processor.process(bom);

        assertEquals(3, component.getProperties().size());
        Optional<Property> errataToolProductName = SbomUtils
                .findPropertyWithNameInComponent("errata-tool-product-name", component);
        assertTrue(errataToolProductName.isPresent());
        assertEquals("pName", errataToolProductName.get().getValue());
        Optional<Property> errataToolProductVersion = SbomUtils
                .findPropertyWithNameInComponent("errata-tool-product-version", component);
        assertTrue(errataToolProductVersion.isPresent());
        assertEquals("pVersion", errataToolProductVersion.get().getValue());
        Optional<Property> errataToolProductVariant = SbomUtils
                .findPropertyWithNameInComponent("errata-tool-product-variant", component);
        assertTrue(errataToolProductVariant.isPresent());
        assertEquals("pVariant", errataToolProductVariant.get().getValue());
    }

    @Test
    void addOnlyIfMissing() {
        Bom bom = new Bom();
        Component component = new Component();

        Property property = new Property();
        property.setName("errata-tool-product-name");
        property.setValue("original-value");

        component.addProperty(property);

        Metadata metadata = new Metadata();
        metadata.setComponent(component);

        bom.setMetadata(metadata);

        processor.process(bom);

        assertEquals(3, component.getProperties().size());
        Optional<Property> errataToolProductName = SbomUtils
                .findPropertyWithNameInComponent("errata-tool-product-name", component);
        assertTrue(errataToolProductName.isPresent());
        assertEquals("original-value", errataToolProductName.get().getValue());
        Optional<Property> errataToolProductVersion = SbomUtils
                .findPropertyWithNameInComponent("errata-tool-product-version", component);
        assertTrue(errataToolProductVersion.isPresent());
        assertEquals("pVersion", errataToolProductVersion.get().getValue());
        Optional<Property> errataToolProductVariant = SbomUtils
                .findPropertyWithNameInComponent("errata-tool-product-variant", component);
        assertTrue(errataToolProductVariant.isPresent());
        assertEquals("pVariant", errataToolProductVariant.get().getValue());
    }
}
