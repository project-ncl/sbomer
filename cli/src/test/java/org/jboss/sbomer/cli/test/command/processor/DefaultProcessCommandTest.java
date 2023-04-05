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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import org.cyclonedx.model.Bom;
import org.cyclonedx.model.Commit;
import org.cyclonedx.model.Component;
import org.cyclonedx.model.ExternalReference;
import org.hamcrest.CoreMatchers;
import org.jboss.pnc.dto.Artifact;
import org.jboss.pnc.dto.Build;
import org.jboss.pnc.dto.Environment;
import org.jboss.pnc.dto.SCMRepository;
import org.jboss.sbomer.cli.CLI;
import org.jboss.sbomer.cli.commands.processor.DefaultProcessCommand;
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
public class DefaultProcessCommandTest extends DefaultProcessCommand {
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

    private void assertExternalReference(Component component, ExternalReference.Type type, String value) {
        assertThat(
                SbomUtils.getExternalReferences(component, type).stream().map(ref -> ref.getUrl()).toList(),
                CoreMatchers.hasItem(value));
    }

    @Test
    void shouldReturnCorrectImplementationType() {
        log.info("test: shouldReturnCorrectImplementationType");
        assertEquals(ProcessorImplementation.DEFAULT, this.getImplementationType());
    }

    @Test
    void shouldManipulateBomContent() throws Exception {
        log.info("test: shouldManipulateBomContent");

        Mockito.when(pncService.getApiUrl()).thenReturn("apiurl");

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
        }

        doProcess(bom);

        for (String purl : componentPurls) {
            Optional<Component> componentOpt = SbomUtils.findComponentWithPurl(purl, bom);
            assertTrue(componentOpt.isPresent());
            Component component = componentOpt.get();

            assertEquals(2, component.getProperties().size());

            assertExternalReference(
                    component,
                    ExternalReference.Type.BUILD_SYSTEM,
                    "https://apiurl/pnc-rest/v2/builds/BBVVCC");
            assertExternalReference(
                    component,
                    ExternalReference.Type.DISTRIBUTION,
                    "https://maven.repository.redhat.com/ga/");
            assertExternalReference(component, ExternalReference.Type.BUILD_META, "systemImageRepositoryUrl/imageid");

            assertEquals("Red Hat", component.getPublisher());
            assertEquals("Red Hat", component.getSupplier().getName());
            assertEquals(List.of("https://www.redhat.com"), component.getSupplier().getUrls());

            Commit commit = component.getPedigree().getCommits().get(0);

            assertEquals("scmrevision", commit.getUid());
            assertEquals("scmurl#scmtag", commit.getUrl());
        }
    }
}
