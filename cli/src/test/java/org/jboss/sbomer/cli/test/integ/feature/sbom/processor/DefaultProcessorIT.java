package org.jboss.sbomer.cli.test.integ.feature.sbom.processor;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.cyclonedx.model.Bom;
import org.cyclonedx.model.Component;
import org.cyclonedx.model.ExternalReference.Type;
import org.cyclonedx.model.Hash;
import org.cyclonedx.model.Hash.Algorithm;
import org.cyclonedx.model.Metadata;
import org.jboss.sbomer.cli.feature.sbom.processor.DefaultProcessor;
import org.jboss.sbomer.cli.test.utils.PncWireMock;
import org.jboss.sbomer.core.features.sbom.utils.SbomUtils;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
@QuarkusTestResource(PncWireMock.class)
class DefaultProcessorIT {

    @Inject
    DefaultProcessor defaultProcessor;

    @Test
    void testProcessing() throws JsonProcessingException {
        // Prepare BOM!
        Bom bom = new Bom();

        Component component = new Component();
        component.setPurl("pkg:maven/org.ow2.asm/asm@9.1.0.redhat-00002?type=jar");
        component.setVersion("9.1.0.redhat-00002");
        component.setHashes(List.of(new Hash(Algorithm.SHA1, "2cdf6b457191ed82ef3a9d2e579f6d0aa495a533")));

        Metadata metadata = new Metadata();
        metadata.setComponent(component);

        bom.setMetadata(metadata);

        // Process!
        defaultProcessor.process(bom);

        assertEquals("Red Hat", component.getSupplier().getName());
        assertEquals("Red Hat", component.getPublisher());
        assertEquals("Red Hat", component.getPublisher());

        assertEquals(
                "https://localhost:12388/pnc-rest/v2/builds/APT4PH2ILMAAA",
                SbomUtils.getExternalReferences(component, Type.BUILD_SYSTEM, "pnc-build-id").get(0).getUrl());
    }

}
