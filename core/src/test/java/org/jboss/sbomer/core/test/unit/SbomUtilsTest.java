/*
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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.cyclonedx.model.Bom;
import org.cyclonedx.model.Component;
import org.cyclonedx.model.Component.Type;
import org.cyclonedx.model.Dependency;
import org.cyclonedx.model.ExternalReference;
import org.cyclonedx.model.Hash;
import org.cyclonedx.model.Hash.Algorithm;
import org.cyclonedx.model.Metadata;
import org.cyclonedx.model.Property;
import org.jboss.pnc.dto.Build;
import org.jboss.pnc.dto.BuildConfigurationRevision;
import org.jboss.pnc.dto.Environment;
import org.jboss.pnc.dto.SCMRepository;
import org.jboss.pnc.enums.SystemImageType;
import org.jboss.sbomer.core.features.sbom.Constants;
import org.jboss.sbomer.core.features.sbom.utils.SbomUtils;
import org.jboss.sbomer.core.test.TestResources;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;

class SbomUtilsTest {

    static Path sbomPath(String fileName) {
        return Paths.get("src", "test", "resources", "sboms", fileName);
    }

    @Nested
    @DisplayName("SbomUtils")
    class SbomUtilsTestNested {
        @Test
        void shouldReadSbomFromFile() {
            Bom bom = SbomUtils.fromPath(sbomPath("base.json"));
            assertNotNull(bom);
            List<Component> components = bom.getComponents();
            assertEquals(39, components.size());

            assertEquals("Apache-2.0", bom.getMetadata().getComponent().getLicenses().getLicenses().get(0).getId());
        }

        @Test
        void shouldReadSbomFromString() throws Exception {
            String bomStr = TestResources.asString(sbomPath("base.json"));
            Bom bom = SbomUtils.fromString(bomStr);
            assertNotNull(bom);
            assertEquals(39, bom.getComponents().size());

            assertEquals("Apache-2.0", bom.getMetadata().getComponent().getLicenses().getLicenses().get(0).getId());
        }

        @Test
        @Disabled("Doesn't work in Temurin JDK, needs investigation")
        // SbomUtilsTest$SbomUtilsTestNested.shouldReadFromFileAndConvertToJsonNode:92 expected:
        // <org.cyclonedx.model.Bom@4e484f54> but was: <org.cyclonedx.model.Bom@c2dbb0fd>
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
            assertNotNull(bom);
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
            assertDoesNotThrow(() -> SbomUtils.removeErrataProperties((Bom) null));
        }

        @Test
        void shouldNotFailOnNullFindProperty() {
            assertEquals(Optional.empty(), SbomUtils.findPropertyWithNameInComponent("blah", null));
        }

        @Test
        void shouldNotFailOnNullPropertiesFindProperty() {
            Component component = new Component();
            assertEquals(Optional.empty(), SbomUtils.findPropertyWithNameInComponent("blah", component));
        }

        @Test
        void shouldNotFindUnmatchedCaseOfPropertyName() {
            Component component = new Component();
            SbomUtils.addProperty(component, "sbomer:image:labels:RELEASE", "main");
            SbomUtils.addProperty(component, "sbomer:image:labels:release", "855");
            assertEquals(2, component.getProperties().size());
            Property uppercase = SbomUtils.findPropertyWithNameInComponent("sbomer:image:labels:RELEASE", component)
                    .orElse(null);
            Property lowercase = SbomUtils.findPropertyWithNameInComponent("sbomer:image:labels:release", component)
                    .orElse(null);
            assertNotNull(uppercase);
            assertNotNull(lowercase);
            assertEquals("main", uppercase.getValue());
            assertEquals("855", lowercase.getValue());
        }

        @Test
        void shouldNotFailOnEmptyPropertiesFindProperty() {
            Component component = new Component();
            component.setProperties(List.of());
            assertEquals(Optional.empty(), SbomUtils.findPropertyWithNameInComponent("blah", component));
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

    @Test
    void testUpdatePurl() {
        Bom bom = new Bom();

        // Main component in metadata
        Component main = new Component();
        main.setPurl("pkg:maven/main-product/asm@9.1.0.redhat-00002?type=jar");
        main.setVersion("9.1.0.redhat-00002");
        main.setHashes(List.of(new Hash(Algorithm.SHA1, "2cdf6b457191ed82ef3a9d2e579f6d0aa495a533")));

        // Regular component with old purl (to be updated)
        Component old = new Component();
        old.setPurl("pkg:maven/org.objectweb.asm/asm@9.1.0.redhat-00002?type=jar");
        old.setVersion("9.1.0.redhat-00002");
        old.setHashes(List.of(new Hash(Algorithm.SHA1, "2cdf6b457191ed82ef3a9d2e579f6d0aa495a533")));

        // Regular component with new metadata (to see if we can handle duplicates)
        Component duplicate = new Component();
        duplicate.setPurl("pkg:maven/org.ow2.asm/asm@9.1.0.redhat-00002?type=jar");
        duplicate.setVersion("9.1.0.redhat-00002");
        duplicate.setHashes(List.of(new Hash(Algorithm.SHA1, "2cdf6b457191ed82ef3a9d2e579f6d0aa495a533")));

        Component other = new Component();
        other.setPurl("pkg:maven/custom@1.1.0.redhat-00002?type=jar");
        other.setVersion("1.1.0.redhat-00002");
        other.setHashes(List.of(new Hash(Algorithm.SHA1, "aaabbbccc")));

        Metadata metadata = new Metadata();
        metadata.setComponent(main);

        bom.setMetadata(metadata);

        List<Component> components = new ArrayList<>();
        components.add(old);
        components.add(duplicate);
        components.add(other);

        bom.setComponents(components);

        // List of dependencies
        List<Dependency> dependencies = new ArrayList<>();

        // Main component as a dependency
        Dependency mainDependency = new Dependency(main.getPurl());

        // All other components added as dependencies to the main component dependency
        mainDependency.addDependency(new Dependency(old.getPurl()));
        mainDependency.addDependency(new Dependency(duplicate.getPurl()));
        mainDependency.addDependency(new Dependency(other.getPurl()));

        // And all components with empty dependency list
        dependencies.add(mainDependency);
        dependencies.add(new Dependency(old.getPurl()));
        dependencies.add(new Dependency(duplicate.getPurl()));
        dependencies.add(new Dependency(other.getPurl()));

        bom.setDependencies(dependencies);

        SbomUtils.updatePurl(
                bom,
                "pkg:maven/org.objectweb.asm/asm@9.1.0.redhat-00002?type=jar",
                "pkg:maven/org.ow2.asm/asm@9.1.0.redhat-00002?type=jar");

        SbomUtils.updatePurl(
                bom,
                "pkg:maven/main-product/asm@9.1.0.redhat-00002?type=jar",
                "pkg:maven/main-product-updated/asm@9.1.0.redhat-00002?type=jar");

        assertEquals(3, bom.getComponents().size());
        assertEquals("pkg:maven/org.ow2.asm/asm@9.1.0.redhat-00002?type=jar", bom.getComponents().get(0).getPurl());
        assertEquals("pkg:maven/org.ow2.asm/asm@9.1.0.redhat-00002?type=jar", bom.getComponents().get(1).getPurl());
        assertEquals("pkg:maven/custom@1.1.0.redhat-00002?type=jar", bom.getComponents().get(2).getPurl());

        // The main component's purl should be updated
        assertEquals(
                "pkg:maven/main-product-updated/asm@9.1.0.redhat-00002?type=jar",
                bom.getMetadata().getComponent().getPurl());

        assertEquals(4, bom.getDependencies().size());

        // Dependencies after update
        assertEquals("pkg:maven/main-product/asm@9.1.0.redhat-00002?type=jar", bom.getDependencies().get(0).getRef());

        assertEquals(
                "pkg:maven/org.objectweb.asm/asm@9.1.0.redhat-00002?type=jar",
                bom.getDependencies().get(1).getRef());
        assertEquals("pkg:maven/org.ow2.asm/asm@9.1.0.redhat-00002?type=jar", bom.getDependencies().get(2).getRef());
        assertEquals("pkg:maven/custom@1.1.0.redhat-00002?type=jar", bom.getDependencies().get(3).getRef());
        assertNull(bom.getDependencies().get(1).getDependencies());
        assertEquals("pkg:maven/org.ow2.asm/asm@9.1.0.redhat-00002?type=jar", bom.getDependencies().get(2).getRef());
        assertNull(bom.getDependencies().get(2).getDependencies());
        assertEquals("pkg:maven/custom@1.1.0.redhat-00002?type=jar", bom.getDependencies().get(3).getRef());
        assertNull(bom.getDependencies().get(3).getDependencies());

        List<Dependency> productDeps = bom.getDependencies().get(0).getDependencies();

        assertEquals(3, productDeps.size());
        assertEquals("pkg:maven/org.objectweb.asm/asm@9.1.0.redhat-00002?type=jar", productDeps.get(0).getRef());
        assertEquals("pkg:maven/org.ow2.asm/asm@9.1.0.redhat-00002?type=jar", productDeps.get(1).getRef());
        assertEquals("pkg:maven/custom@1.1.0.redhat-00002?type=jar", productDeps.get(2).getRef());
    }
}
