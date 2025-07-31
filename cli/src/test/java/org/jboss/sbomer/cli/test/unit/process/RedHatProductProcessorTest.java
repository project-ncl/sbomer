package org.jboss.sbomer.cli.test.unit.process;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;

import org.cyclonedx.model.Bom;
import org.cyclonedx.model.Component;
import org.cyclonedx.model.Metadata;
import org.cyclonedx.model.Property;
import org.jboss.sbomer.cli.feature.sbom.processor.RedHatProductProcessor;
import org.jboss.sbomer.core.features.sbom.Constants;
import org.jboss.sbomer.core.features.sbom.utils.SbomUtils;
import org.junit.jupiter.api.Test;

class RedHatProductProcessorTest {
    final RedHatProductProcessor processor = new RedHatProductProcessor("pName", "pVersion", "pVariant");

    @Test
    void addAll() {
        Bom bom = new Bom();
        Component metadataComponent = new Component();
        Component mainComponent = new Component();

        Metadata metadata = new Metadata();
        metadata.setComponent(metadataComponent);

        bom.setMetadata(metadata);
        bom.setComponents(List.of(mainComponent));

        processor.process(bom);

        assertEquals(3, mainComponent.getProperties().size());
        Optional<Property> errataToolProductName = SbomUtils
                .findPropertyWithNameInComponent(Constants.PROPERTY_ERRATA_PRODUCT_NAME, mainComponent);
        assertTrue(errataToolProductName.isPresent());
        assertEquals("pName", errataToolProductName.get().getValue());
        Optional<Property> errataToolProductVersion = SbomUtils
                .findPropertyWithNameInComponent(Constants.PROPERTY_ERRATA_PRODUCT_VERSION, mainComponent);
        assertTrue(errataToolProductVersion.isPresent());
        assertEquals("pVersion", errataToolProductVersion.get().getValue());
        Optional<Property> errataToolProductVariant = SbomUtils
                .findPropertyWithNameInComponent(Constants.PROPERTY_ERRATA_PRODUCT_VARIANT, mainComponent);
        assertTrue(errataToolProductVariant.isPresent());
        assertEquals("pVariant", errataToolProductVariant.get().getValue());
    }

    @Test
    void addOnlyIfMissing() {
        Bom bom = new Bom();
        Component metadataComponent = new Component();
        Component mainComponent = new Component();

        Property property = new Property();
        property.setName(Constants.PROPERTY_ERRATA_PRODUCT_NAME);
        property.setValue("original-value");

        mainComponent.addProperty(property);

        Metadata metadata = new Metadata();
        metadata.setComponent(metadataComponent);

        bom.setMetadata(metadata);
        bom.setComponents(List.of(mainComponent));

        processor.process(bom);

        assertEquals(3, mainComponent.getProperties().size());
        Optional<Property> errataToolProductName = SbomUtils
                .findPropertyWithNameInComponent(Constants.PROPERTY_ERRATA_PRODUCT_NAME, mainComponent);
        assertTrue(errataToolProductName.isPresent());
        assertEquals("original-value", errataToolProductName.get().getValue());
        Optional<Property> errataToolProductVersion = SbomUtils
                .findPropertyWithNameInComponent(Constants.PROPERTY_ERRATA_PRODUCT_VERSION, mainComponent);
        assertTrue(errataToolProductVersion.isPresent());
        assertEquals("pVersion", errataToolProductVersion.get().getValue());
        Optional<Property> errataToolProductVariant = SbomUtils
                .findPropertyWithNameInComponent(Constants.PROPERTY_ERRATA_PRODUCT_VARIANT, mainComponent);
        assertTrue(errataToolProductVariant.isPresent());
        assertEquals("pVariant", errataToolProductVariant.get().getValue());
    }
}
