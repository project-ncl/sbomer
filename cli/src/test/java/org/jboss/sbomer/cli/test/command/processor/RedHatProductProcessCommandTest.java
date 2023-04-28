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
package org.jboss.sbomer.cli.test.command.processor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import javax.inject.Inject;

import org.cyclonedx.model.Bom;
import org.jboss.pnc.dto.Build;
import org.jboss.pnc.dto.BuildConfiguration;
import org.jboss.pnc.dto.BuildConfigurationRevisionRef;
import org.jboss.pnc.dto.Environment;
import org.jboss.pnc.dto.ProductVersionRef;
import org.jboss.pnc.dto.SCMRepository;
import org.jboss.sbomer.cli.commands.processor.RedHatProductProcessCommand;
import org.jboss.sbomer.cli.model.Sbom;
import org.jboss.sbomer.core.enums.ProcessorImplementation;
import org.jboss.sbomer.core.errors.ApplicationException;
import org.jboss.sbomer.core.service.PncService;
import org.jboss.sbomer.core.test.TestResources;
import org.jboss.sbomer.core.utils.SbomUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;

@QuarkusTest
public class RedHatProductProcessCommandTest {
    @Inject
    RedHatProductProcessCommand command;

    @Inject
    ObjectMapper objectMapper;

    @InjectMock
    PncService pncService;

    private JsonNode generateBom() throws IOException {
        String bomJson = TestResources.asString("sboms/sbom-valid.json");
        return objectMapper.readTree(bomJson);
    }

    private Build generateBuild() {
        return Build.builder()
                .id("BBVVCC")
                .environment(
                        Environment.builder()
                                .systemImageId("imageid")
                                .systemImageRepositoryUrl("systemImageRepositoryUrl")
                                .build())
                .scmRepository(SCMRepository.builder().externalUrl("externalurl").build())
                .scmTag("scmtag")
                .scmRevision("scmrevision")
                .scmUrl("scmurl")
                .buildConfigRevision(BuildConfigurationRevisionRef.refBuilder().id("BCID").build())
                .build();
    }

    private Sbom generateSbom() throws IOException {
        return Sbom.builder().buildId("BBVVCC").sbom(generateBom()).id(123456l).build();
    }

    @Test
    void shouldReturnCorrectImplementationType() {
        assertEquals(ProcessorImplementation.REDHAT_PRODUCT, command.getImplementationType());
    }

    @Test
    void shouldStopProcessingIfTheBuildIsNotFound() throws Exception {
        Sbom sbom = generateSbom();

        ApplicationException ex = Assertions.assertThrows(ApplicationException.class, () -> {
            command.doProcess(sbom);
        });

        assertEquals("Build related to the SBOM could not be found in PNC, interrupting processing", ex.getMessage());
    }

    @Test
    void generatedSbomShouldNotHaveProductMetadata() throws IOException {
        Sbom sbom = generateSbom();

        Bom bom = SbomUtils.fromJsonNode(sbom.getSbom());

        assertFalse(
                SbomUtils.hasProperty(
                        bom.getMetadata().getComponent(),
                        RedHatProductProcessCommand.PROPERTY_ERRATA_PRODUCT_NAME));
        assertFalse(
                SbomUtils.hasProperty(
                        bom.getMetadata().getComponent(),
                        RedHatProductProcessCommand.PROPERTY_ERRATA_PRODUCT_VERSION));
        assertFalse(
                SbomUtils.hasProperty(
                        bom.getMetadata().getComponent(),
                        RedHatProductProcessCommand.PROPERTY_ERRATA_PRODUCT_VARIANT));
    }

    @Test
    void shouldStopWhenBuildConfigIsNotFound() throws Exception {
        Mockito.when(pncService.getBuild("BBVVCC")).thenReturn(generateBuild());

        Sbom sbom = generateSbom();
        ApplicationException ex = Assertions.assertThrows(ApplicationException.class, () -> {
            command.doProcess(sbom);
        });

        assertEquals(
                "BuildConfig related to the SBOM could not be found in PNC, interrupting processing",
                ex.getMessage());
    }

    @Test
    void shouldFailOnMissingMapping() throws Exception {
        Mockito.when(pncService.getBuild("BBVVCC")).thenReturn(generateBuild());
        Mockito.when(pncService.getBuildConfig("BCID"))
                .thenReturn(
                        BuildConfiguration.builder()
                                .productVersion(ProductVersionRef.refBuilder().id("PVID").version("PV").build())
                                .build());

        Sbom sbom = generateSbom();

        ApplicationException ex = Assertions.assertThrows(ApplicationException.class, () -> {
            command.doProcess(sbom);
        });

        assertEquals("Could not find mapping for the PNC Product Version 'PV' (id: PVID)", ex.getMessage());
    }

    @Test
    @DisplayName("Should run the processor successfully")
    void shouldProcess() throws Exception {
        Mockito.when(pncService.getBuild("BBVVCC")).thenReturn(generateBuild());
        Mockito.when(pncService.getBuildConfig("BCID"))
                .thenReturn(
                        BuildConfiguration.builder()
                                .productVersion(ProductVersionRef.refBuilder().id("377").version("PV").build())
                                .build());

        Sbom sbom = generateSbom();
        Bom bom = command.doProcess(sbom);

        assertTrue(
                SbomUtils.hasProperty(
                        bom.getMetadata().getComponent(),
                        RedHatProductProcessCommand.PROPERTY_ERRATA_PRODUCT_NAME));
        assertTrue(
                SbomUtils.hasProperty(
                        bom.getMetadata().getComponent(),
                        RedHatProductProcessCommand.PROPERTY_ERRATA_PRODUCT_VERSION));
        assertTrue(
                SbomUtils.hasProperty(
                        bom.getMetadata().getComponent(),
                        RedHatProductProcessCommand.PROPERTY_ERRATA_PRODUCT_VARIANT));

        assertEquals(
                "RHBQ",
                SbomUtils
                        .findPropertyWithNameInComponent(
                                RedHatProductProcessCommand.PROPERTY_ERRATA_PRODUCT_NAME,
                                bom.getMetadata().getComponent())
                        .get()
                        .getValue());
        assertEquals(
                "RHEL-8-RHBQ-2.13",
                SbomUtils
                        .findPropertyWithNameInComponent(
                                RedHatProductProcessCommand.PROPERTY_ERRATA_PRODUCT_VERSION,
                                bom.getMetadata().getComponent())
                        .get()
                        .getValue());
        assertEquals(
                "8Base-RHBQ-2.13",
                SbomUtils
                        .findPropertyWithNameInComponent(
                                RedHatProductProcessCommand.PROPERTY_ERRATA_PRODUCT_VARIANT,
                                bom.getMetadata().getComponent())
                        .get()
                        .getValue());
    }

    @Test
    @DisplayName("Should interrupt processing on missing product version")
    void shouldMissingProductVersion() throws Exception {
        Mockito.when(pncService.getBuild("BBVVCC")).thenReturn(generateBuild());
        Mockito.when(pncService.getBuildConfig("BCID")).thenReturn(BuildConfiguration.builder().build());

        Sbom sbom = generateSbom();

        ApplicationException ex = Assertions.assertThrows(ApplicationException.class, () -> {
            command.doProcess(sbom);
        });

        assertEquals(
                "BuildConfig related to the SBOM does not provide product version information, interrupting processing",
                ex.getMessage());
    }

}
