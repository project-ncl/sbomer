package org.jboss.sbomer.cli.test.unit.process;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
        assertEquals(
                "pName",
                SbomUtils.findPropertyWithNameInComponent("errata-tool-product-name", component).get().getValue());
        assertEquals(
                "pVersion",
                SbomUtils.findPropertyWithNameInComponent("errata-tool-product-version", component).get().getValue());
        assertEquals(
                "pVariant",
                SbomUtils.findPropertyWithNameInComponent("errata-tool-product-variant", component).get().getValue());
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
        assertEquals(
                "original-value",
                SbomUtils.findPropertyWithNameInComponent("errata-tool-product-name", component).get().getValue());
        assertEquals(
                "pVersion",
                SbomUtils.findPropertyWithNameInComponent("errata-tool-product-version", component).get().getValue());
        assertEquals(
                "pVariant",
                SbomUtils.findPropertyWithNameInComponent("errata-tool-product-variant", component).get().getValue());
    }
}
