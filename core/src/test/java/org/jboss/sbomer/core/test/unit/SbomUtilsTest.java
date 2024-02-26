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
package org.jboss.sbomer.core.test.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

import org.cyclonedx.model.Bom;
import org.cyclonedx.model.Component;
import org.cyclonedx.model.Dependency;
import org.cyclonedx.model.ExternalReference;
import org.cyclonedx.model.Component.Type;
import org.jboss.pnc.dto.Build;
import org.jboss.pnc.dto.BuildConfigurationRevision;
import org.jboss.pnc.dto.Environment;
import org.jboss.pnc.dto.SCMRepository;
import org.jboss.pnc.enums.SystemImageType;
import org.jboss.sbomer.core.features.sbom.Constants;
import org.jboss.sbomer.core.features.sbom.utils.SbomUtils;
import org.jboss.sbomer.core.test.TestResources;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;

public class SbomUtilsTest {

    static Path sbomPath(String fileName) {
        return Paths.get("src", "test", "resources", "sboms", fileName);
    }

    @Nested
    @DisplayName("SbomUtils")
    class SbomUtilsTestNested {
        @Test
        void shouldReadSbomFromFile() throws Exception {
            Bom bom = SbomUtils.fromPath(sbomPath("base.json"));

            assertEquals(39, bom.getComponents().size());

            assertEquals(
                    "Apache-2.0",
                    bom.getMetadata().getComponent().getLicenseChoice().getLicenses().get(0).getId());
        }

        @Test
        void shouldReadSbomFromString() throws Exception {
            String bomStr = TestResources.asString(sbomPath("base.json"));
            Bom bom = SbomUtils.fromString(bomStr);

            assertEquals(39, bom.getComponents().size());

            assertEquals(
                    "Apache-2.0",
                    bom.getMetadata().getComponent().getLicenseChoice().getLicenses().get(0).getId());
        }

        @Test
        void shouldReadFromFileAndConvertToJsonNode() {
            Bom bom = SbomUtils.fromPath(sbomPath("base.json"));
            JsonNode jsonNode = SbomUtils.toJsonNode(bom);
            Bom bom2 = SbomUtils.fromJsonNode(jsonNode);
            assertEquals(bom, bom2);

            JsonNode licenses = jsonNode.get("metadata").get("component").get("licenses");

            assertNotNull(licenses);
            assertEquals(JsonNodeType.ARRAY, licenses.getNodeType());

            JsonNode license = licenses.get(0);
            assertNotNull(license);
            assertEquals("Apache-2.0", license.get("license").get("id").asText());
        }

        @Test
        void shouldRemoveErrataPropertiesFromBom() {
            Bom bom = SbomUtils.fromPath(sbomPath("sbom_with_errata.json"));
            assertNotNull(bom.getMetadata().getComponent());

            assertTrue(SbomUtils.hasProperty(bom.getMetadata().getComponent(), Constants.PROPERTY_ERRATA_PRODUCT_NAME));
            assertTrue(
                    SbomUtils.hasProperty(bom.getMetadata().getComponent(), Constants.PROPERTY_ERRATA_PRODUCT_VARIANT));
            assertTrue(
                    SbomUtils.hasProperty(bom.getMetadata().getComponent(), Constants.PROPERTY_ERRATA_PRODUCT_VERSION));

            SbomUtils.removeErrataProperties(bom);

            assertFalse(
                    SbomUtils.hasProperty(bom.getMetadata().getComponent(), Constants.PROPERTY_ERRATA_PRODUCT_NAME));
            assertFalse(
                    SbomUtils.hasProperty(bom.getMetadata().getComponent(), Constants.PROPERTY_ERRATA_PRODUCT_VARIANT));
            assertFalse(
                    SbomUtils.hasProperty(bom.getMetadata().getComponent(), Constants.PROPERTY_ERRATA_PRODUCT_VERSION));
        }

        @Test
        void shouldRemoveErrataPropertiesFromJsonNode() {
            Bom bom = SbomUtils.fromPath(sbomPath("sbom_with_errata.json"));
            JsonNode jsonNode = SbomUtils.toJsonNode(bom);

            JsonNode properties = jsonNode.get("metadata").get("component").get("properties");
            assertNotNull(properties);
            assertEquals(JsonNodeType.ARRAY, properties.getNodeType());

            boolean foundErrataProductName = false;
            boolean foundErrataProductVersion = false;
            boolean foundErrataProductVariant = false;
            for (JsonNode node : properties) {
                if (node.has("name")) {
                    String name = node.get("name").asText();
                    if (Constants.PROPERTY_ERRATA_PRODUCT_NAME.equals(name)) {
                        foundErrataProductName = true;
                    }
                    if (Constants.PROPERTY_ERRATA_PRODUCT_VERSION.equals(name)) {
                        foundErrataProductVersion = true;
                    }
                    if (Constants.PROPERTY_ERRATA_PRODUCT_VARIANT.equals(name)) {
                        foundErrataProductVariant = true;
                    }
                }
            }

            assertTrue(foundErrataProductName);
            assertTrue(foundErrataProductVersion);
            assertTrue(foundErrataProductVariant);
            assertEquals(3, properties.size());

            jsonNode = SbomUtils.removeErrataProperties(jsonNode);

            properties = jsonNode.get("metadata").get("component").get("properties");
            assertNull(properties);
        }

        @Test
        void shouldNotFailOnNullRemoveErrataProperties() {
            Bom bom = null;
            SbomUtils.removeErrataProperties(bom);
        }

        @Test
        void shouldNotFailOnNullFindProperty() {
            assertEquals(Optional.ofNullable(null), SbomUtils.findPropertyWithNameInComponent("blah", null));
        }

        @Test
        void shouldNotFailOnNullPropertiesFindProperty() {
            Component component = new Component();
            assertEquals(Optional.ofNullable(null), SbomUtils.findPropertyWithNameInComponent("blah", component));
        }

        @Test
        void shouldNotFailOnEmptyPropertiesFindProperty() {
            Component component = new Component();
            component.setProperties(List.of());
            assertEquals(Optional.ofNullable(null), SbomUtils.findPropertyWithNameInComponent("blah", component));
        }

        @Test
        void shouldHandleRegularProtocolUrlsForPedigrees() {
            Component component = new Component();
            SbomUtils.addPedigreeCommit(
                    component,
                    "https://github.com/FasterXML/jackson-annotations.git",
                    "aaabbbcccdddeee");

            assertEquals(
                    "https://github.com/FasterXML/jackson-annotations.git",
                    component.getPedigree().getCommits().get(0).getUrl());
        }

        @Test
        void shouldHandleGitProtocolUrlsForPedigrees() {
            Component component = new Component();
            SbomUtils.addPedigreeCommit(
                    component,
                    "git@github.com:FasterXML/jackson-annotations.git",
                    "aaabbbcccdddeee");

            assertEquals(
                    "https://github.com/FasterXML/jackson-annotations.git",
                    component.getPedigree().getCommits().get(0).getUrl());
        }

        @Test
        void shouldCreateComponent() {
            Component component = SbomUtils.createComponent(
                    "broker",
                    "7.11.5.CR3",
                    "broker-7.11.5.CR3-maven-repository.zip",
                    "SBOM representing the deliverable my-7.11.5.CR3-maven-repository.zip",
                    "pkg:generic/broker@7.11.5.CR3?download_url=https://download.com/my-7.11.5.CR3-maven-repository.zip",
                    Type.FILE);
            assertEquals("broker", component.getGroup());
            assertEquals("7.11.5.CR3", component.getName());
            assertEquals("broker-7.11.5.CR3-maven-repository.zip", component.getVersion());
            assertEquals(
                    "SBOM representing the deliverable my-7.11.5.CR3-maven-repository.zip",
                    component.getDescription());
            assertEquals(
                    "pkg:generic/broker@7.11.5.CR3?download_url=https://download.com/my-7.11.5.CR3-maven-repository.zip",
                    component.getPurl());
            assertEquals(component.getBomRef(), component.getPurl());
            assertEquals(Type.FILE, component.getType());
        }

        @Test
        void shouldCreateMetadata() {
            String ref = "pkg:generic/broker@7.11.5.CR3?download_url=https://download.com/my-7.11.5.CR3-maven-repository.zip";
            Dependency dependency = SbomUtils.createDependency(ref);
            assertEquals(ref, dependency.getRef());
            assertNull(dependency.getDependencies());
        }

        @Test
        void shouldSetPncMetadata() {
            String pncApiUrl = "pncApiUrl.com";

            String envName = "OpenJDK 1.8; Mvn 3.6.0; Gradle 5.6.2";
            String envSystemImageRepositoryUrl = "quay.io/rh-newcastle";
            String envSystemImageId = "builder-rhel-7-j8-mvn3.6.0-gradle5.6.2:1.0.8";
            Environment environment = Environment.builder()
                    .name(envName)
                    .systemImageId(envSystemImageId)
                    .systemImageRepositoryUrl(envSystemImageRepositoryUrl)
                    .systemImageType(SystemImageType.DOCKER_IMAGE)
                    .build();

            String scmInternalUrl = "git+ssh://code.com/cpaas/cpaas-test-pnc-gradle.git";
            String scmExternalUrl = "https://gitlab.cee.redhat.com/ncross/cpaas-test-pnc-gradle.git";
            SCMRepository scmRepository = SCMRepository.builder()
                    .internalUrl(scmInternalUrl)
                    .externalUrl(scmExternalUrl)
                    .build();

            String scmUrl = "https://code.engineering.redhat.com/gerrit/cpaas/cpaas-test-pnc-gradle.git";
            String scmRevision = "c8ecca0d966250c5caef8174a20a4f1f1f50e6d7";
            String scmTag = "1.0.0.redhat-05289";
            String scmBuildConfigRevision = "e08bf4d4d3c09ef38ec4e4bd5ddfccf5f51d6168";
            boolean scmInternal = false;

            BuildConfigurationRevision buildConfigurationRevision = BuildConfigurationRevision.builder()
                    .scmRevision(scmBuildConfigRevision)
                    .build();

            String buildId = "13";
            Build build = Build.builder()
                    .id(buildId)
                    .environment(environment)
                    .scmRepository(scmRepository)
                    .scmBuildConfigRevision(scmBuildConfigRevision)
                    .scmTag(scmTag)
                    .scmUrl(scmUrl)
                    .scmRevision(scmRevision)
                    .buildConfigRevision(buildConfigurationRevision)
                    .build();

            Component component = SbomUtils.setPncBuildMetadata(new Component(), build, pncApiUrl);
            List<ExternalReference> buildSystems = SbomUtils
                    .getExternalReferences(component, ExternalReference.Type.BUILD_SYSTEM);
            assertEquals(1, buildSystems.size());
            assertEquals(ExternalReference.Type.BUILD_SYSTEM, buildSystems.get(0).getType());
            assertEquals("https://" + pncApiUrl + "/pnc-rest/v2/builds/" + buildId, buildSystems.get(0).getUrl());
            assertEquals(Constants.SBOM_RED_HAT_PNC_BUILD_ID, buildSystems.get(0).getComment());

            List<ExternalReference> buildMetadata = SbomUtils
                    .getExternalReferences(component, ExternalReference.Type.BUILD_META);
            assertEquals(1, buildMetadata.size());
            assertEquals(ExternalReference.Type.BUILD_META, buildMetadata.get(0).getType());
            assertEquals(envSystemImageRepositoryUrl + "/" + envSystemImageId, buildMetadata.get(0).getUrl());
            assertEquals(Constants.SBOM_RED_HAT_ENVIRONMENT_IMAGE, buildMetadata.get(0).getComment());

            List<ExternalReference> buildVcs = SbomUtils.getExternalReferences(component, ExternalReference.Type.VCS);
            assertEquals(1, buildVcs.size());
            assertEquals(ExternalReference.Type.VCS, buildVcs.get(0).getType());
            assertEquals(scmExternalUrl, buildVcs.get(0).getUrl());
            assertEquals("", buildVcs.get(0).getComment());
        }

        @Test
        void shouldSetBrewMetadata() {
            String kojiApiUrl = "kojiApiUrl.com";
            String brewId = "1313";

            Component component = SbomUtils.setBrewBuildMetadata(new Component(), brewId, Optional.empty(), kojiApiUrl);
            List<ExternalReference> buildSystems = SbomUtils
                    .getExternalReferences(component, ExternalReference.Type.BUILD_SYSTEM);
            assertEquals(1, buildSystems.size());
            assertEquals(ExternalReference.Type.BUILD_SYSTEM, buildSystems.get(0).getType());
            assertEquals(kojiApiUrl + "/buildinfo?buildID=" + brewId, buildSystems.get(0).getUrl());
            assertEquals(Constants.SBOM_RED_HAT_BREW_BUILD_ID, buildSystems.get(0).getComment());
        }
    }

}
