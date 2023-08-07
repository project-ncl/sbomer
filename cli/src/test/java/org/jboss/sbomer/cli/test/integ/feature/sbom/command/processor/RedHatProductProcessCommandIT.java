/**
 * JBoss, Home of Professional Open Source.
 * Copyright 2023 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.sbomer.cli.test.integ.feature.sbom.command.processor;

import static org.jboss.sbomer.core.features.sbom.Constants.PROPERTY_ERRATA_PRODUCT_NAME;
import static org.jboss.sbomer.core.features.sbom.Constants.PROPERTY_ERRATA_PRODUCT_VARIANT;
import static org.jboss.sbomer.core.features.sbom.Constants.PROPERTY_ERRATA_PRODUCT_VERSION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;

import javax.inject.Inject;

import org.cyclonedx.model.Bom;
import org.jboss.sbomer.cli.feature.sbom.command.RedHatProductProcessCommand;
import org.jboss.sbomer.cli.test.utils.PncWireMock;
import org.jboss.sbomer.core.features.sbom.enums.ProcessorType;
import org.jboss.sbomer.core.features.sbom.utils.SbomUtils;
import org.jboss.sbomer.core.test.TestResources;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@QuarkusTestResource(PncWireMock.class)
public class RedHatProductProcessCommandIT {
    @Inject
    RedHatProductProcessCommand command;

    @Inject
    ObjectMapper objectMapper;

    private JsonNode generateBom() throws IOException {
        String bomJson = TestResources.asString("boms/valid.json");
        return objectMapper.readTree(bomJson);
    }

    @Test
    void shouldReturnCorrectImplementationType() {
        assertEquals(ProcessorType.REDHAT_PRODUCT, command.getImplementationType());
    }

    // TODO: This deosn't make sense after refactoring, but maybe it could be a base for some other test, leaving for
    // now
    // @Test
    // void shouldStopProcessingIfTheBuildIsNotFound() throws Exception {
    // //Sbom sbom = generateSbom();
    // sbom.setBuildId("NOTEXISTING");

    // ApplicationException ex = Assertions.assertThrows(ApplicationException.class, () -> {
    // command.doProcess(SbomUtils.fromJsonNode(generateBom()));
    // });

    // assertEquals(
    // "Could not obtain PNC Product Version information for the 'NOTEXISTING' PNC build, interrupting processing",
    // ex.getMessage());

    // }

    @Test
    void generatedSbomShouldNotHaveProductMetadata() throws IOException {
        Bom bom = SbomUtils.fromJsonNode(generateBom());

        assertFalse(SbomUtils.hasProperty(bom.getMetadata().getComponent(), PROPERTY_ERRATA_PRODUCT_NAME));
        assertFalse(SbomUtils.hasProperty(bom.getMetadata().getComponent(), PROPERTY_ERRATA_PRODUCT_VERSION));
        assertFalse(SbomUtils.hasProperty(bom.getMetadata().getComponent(), PROPERTY_ERRATA_PRODUCT_VARIANT));
    }

    // @Test
    // @DisplayName("Should run the processor successfully")
    // void shouldProcess() throws Exception {
    // // Sbom sbom = generateSbom();
    // // sbom.setBuildId("QUARKUS");
    // Bom bom = command.doProcess(generateBom());

    // assertTrue(SbomUtils.hasProperty(bom.getMetadata().getComponent(), PROPERTY_ERRATA_PRODUCT_NAME));
    // assertTrue(SbomUtils.hasProperty(bom.getMetadata().getComponent(), PROPERTY_ERRATA_PRODUCT_VERSION));
    // assertTrue(SbomUtils.hasProperty(bom.getMetadata().getComponent(), PROPERTY_ERRATA_PRODUCT_VARIANT));

    // assertEquals(
    // "RHBQ",
    // SbomUtils
    // .findPropertyWithNameInComponent(PROPERTY_ERRATA_PRODUCT_NAME, bom.getMetadata().getComponent())
    // .get()
    // .getValue());
    // assertEquals(
    // "RHEL-8-RHBQ-2.13",
    // SbomUtils
    // .findPropertyWithNameInComponent(
    // PROPERTY_ERRATA_PRODUCT_VERSION,
    // bom.getMetadata().getComponent())
    // .get()
    // .getValue());
    // assertEquals(
    // "8Base-RHBQ-2.13",
    // SbomUtils
    // .findPropertyWithNameInComponent(
    // PROPERTY_ERRATA_PRODUCT_VARIANT,
    // bom.getMetadata().getComponent())
    // .get()
    // .getValue());
    // }

    // @Test
    // @DisplayName("Should interrupt processing on missing product version")
    // void shouldMissingProductVersion() throws Exception {
    // Sbom sbom = generateSbom();
    // sbom.setBuildId("MISSINGPRODUCTVERSION");

    // ApplicationException ex = Assertions.assertThrows(ApplicationException.class, () -> {
    // command.doProcess(sbom, SbomUtils.fromJsonNode(sbom.getSbom()));
    // });

    // assertEquals(
    // "Could not obtain PNC Product Version information for the 'MISSINGPRODUCTVERSION' PNC build, interrupting
    // processing",
    // ex.getMessage());
    // }

}
