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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Optional;

import javax.inject.Inject;

import org.cyclonedx.model.Bom;
import org.cyclonedx.model.Component;
import org.cyclonedx.model.Property;
import org.jboss.pnc.dto.Artifact;
import org.jboss.pnc.dto.Build;
import org.jboss.pnc.dto.Environment;
import org.jboss.pnc.dto.SCMRepository;
import org.jboss.sbomer.cli.CLI;
import org.jboss.sbomer.cli.commands.processor.PropertiesProcessCommand;
import org.jboss.sbomer.cli.service.PNCService;
import org.jboss.sbomer.core.enums.ProcessorImplementation;
import org.jboss.sbomer.core.test.TestResources;
import org.jboss.sbomer.core.utils.SbomUtils;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@QuarkusTest
public class PropertiesProcessCommandTest extends PropertiesProcessCommand {
    @Inject
    CLI cli;

    @Inject
    ObjectMapper objectMapper;

    @InjectMock
    PNCService pncService;

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
                .build();
    }

    private Artifact generateArtifact(String purl) {
        return Artifact.builder()
                .id("AA1122")
                .md5("md5")
                .sha1("sha1")
                .sha256("sha256")
                .purl(purl)
                .publicUrl("artifactpublicurl")
                .originUrl("originurl")
                .build(generateBuild())
                .build();
    }

    private void assertPropertyInComponent(String propertyName, String propertyValue, Component component) {
        Optional<Property> property = SbomUtils.findPropertyWithNameInComponent(propertyName, component);
        assertTrue(property.isPresent());
        assertEquals(propertyValue, property.get().getValue());
    }

    @Test
    void shouldReturnCorrectImplementationType() {
        log.info("test: shouldReturnCorrectImplementationType");
        assertEquals(ProcessorImplementation.PROPERTIES, this.getImplementationType());
    }

    @Test
    void shouldManipulateProperties() throws Exception {
        log.info("test: shouldManipulateProperties");
        String[] componentPurls = { "pkg:maven/commons-io/commons-io@2.6.0.redhat-00001?type=jar",
                "pkg:maven/com.aayushatharva.brotli4j/brotli4j@1.8.0.redhat-00003?type=jar",
                "pkg:maven/org.eclipse.microprofile.graphql/microprofile-graphql-api@1.1.0.redhat-00008?type=jar",
                "pkg:maven/org.eclipse.microprofile.graphql/microprofile-graphql-tck@1.1.0.redhat-00008?type=jar" };

        for (String purl : componentPurls) {
            Mockito.when(pncService.getArtifact(purl)).thenReturn(generateArtifact(purl));
        }

        Bom bom = SbomUtils.fromJsonNode(generateBom());

        for (String purl : componentPurls) {
            Optional<Component> componentOpt = SbomUtils.findComponentWithPurl(purl, bom);
            assertTrue(componentOpt.isPresent());
            Component component = componentOpt.get();

            assertEquals(2, component.getProperties().size());

            assertPropertyInComponent("package:type", "maven", component);
            assertPropertyInComponent("package:language", "java", component);
        }

        doProcess(bom);

        for (String purl : componentPurls) {
            Optional<Component> componentOpt = SbomUtils.findComponentWithPurl(purl, bom);
            assertTrue(componentOpt.isPresent());
            Component component = componentOpt.get();

            assertEquals(10, component.getProperties().size());

            assertPropertyInComponent("package:type", "maven", component);
            assertPropertyInComponent("package:language", "java", component);
            assertPropertyInComponent("public-url", "artifactpublicurl", component);
            assertPropertyInComponent("origin-url", "originurl", component);
            assertPropertyInComponent("scm-tag", "scmtag", component);
            assertPropertyInComponent("scm-url", "scmurl", component);
            assertPropertyInComponent("scm-revision", "scmrevision", component);
            assertPropertyInComponent("scm-external-url", "externalurl", component);
            assertPropertyInComponent("pnc-environment-image", "systemImageRepositoryUrl/imageid", component);
            assertPropertyInComponent("pnc-build-id", "BBVVCC", component);
        }
    }
}
