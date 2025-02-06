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
package org.jboss.sbomer.cli.test.unit.feature.sbom;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.cyclonedx.model.Bom;
import org.cyclonedx.model.Component;
import org.cyclonedx.model.Dependency;
import org.jboss.pnc.dto.Artifact;
import org.jboss.pnc.dto.Build;
import org.jboss.sbomer.cli.feature.sbom.processor.WorkaroundMissingNpmDependencies;
import org.jboss.sbomer.core.features.sbom.utils.ObjectMapperProvider;
import org.jboss.sbomer.core.features.sbom.utils.SbomUtils;
import org.jboss.sbomer.core.pnc.PncService;
import org.jboss.sbomer.core.test.TestResources;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WorkaroundMissingNpmDependenciesTest {

    private static final ObjectMapper OBJECT_MAPPER = ObjectMapperProvider.json();

    @Test
    void testNpmDependencyAdded() throws IOException {
        // Mock PNC service
        PncService pncServiceMock = Mockito.mock(PncService.class);
        Build pncBuild = OBJECT_MAPPER.readValue(TestResources.asString("pnc/mavenBuild.json"), Build.class);
        List<Artifact> artifacts = OBJECT_MAPPER
                .readValue(TestResources.asString("pnc/npmDependencies.json"), new TypeReference<>() {
                });
        when(pncServiceMock.getBuild(("FOOBAR012345"))).thenReturn(pncBuild);
        when(pncServiceMock.getNPMDependencies(("FOOBAR012345"))).thenReturn(artifacts);

        // Prepare test component
        Bom bom = Objects.requireNonNull(SbomUtils.createBom());
        Component component1 = SbomUtils.createComponent(
                "foo.bar",
                "baz",
                "1.0.0.redhat-00001",
                "Test project",
                "pkg:maven/foo.bar/baz@1.0.0.redhat-00001?type=jar",
                Component.Type.LIBRARY);
        bom.addComponent(component1);
        bom.addDependency(SbomUtils.createDependency(component1.getBomRef()));
        SbomUtils.setPncBuildMetadata(component1, pncBuild, "pnc.example.com");

        Component component2 = SbomUtils.createComponent(
                "foo.bar",
                "qux",
                "1.0.0.redhat-00001",
                "Test project",
                "pkg:maven/foo.bar/qux@1.0.0.redhat-00001?type=jar",
                Component.Type.LIBRARY);
        bom.addComponent(component2);
        bom.addDependency(SbomUtils.createDependency(component2.getBomRef()));
        SbomUtils.setPncBuildMetadata(component2, pncBuild, "pnc.example.com");

        // Check assertions before test
        assertEquals(2, bom.getComponents().size());
        assertEquals(2, bom.getDependencies().size());

        // Run test
        WorkaroundMissingNpmDependencies workaround = new WorkaroundMissingNpmDependencies(pncServiceMock);
        workaround.analyzeComponentsBuild(component1);
        workaround.analyzeComponentsBuild(component2);
        workaround.addMissingDependencies(bom);

        // Verify after test
        assertEquals(4, bom.getComponents().size());
        assertEquals(4, bom.getDependencies().size());

        Dependency dependency1 = getDependency(component1.getBomRef(), bom.getDependencies()).orElseThrow();
        Dependency dependency2 = getDependency(component2.getBomRef(), bom.getDependencies()).orElseThrow();
        assertEquals(2, dependency1.getDependencies().size());
        assertEquals(2, dependency2.getDependencies().size());
        assertEquals(dependency1.getDependencies(), dependency2.getDependencies());

        assertTrue(getDependency("pkg:npm/once@1.4.0", bom.getDependencies()).isPresent());
        assertTrue(getDependency("pkg:npm/once@1.4.0", dependency1.getDependencies()).isPresent());
        assertTrue(getDependency("pkg:npm/once@1.4.0", dependency2.getDependencies()).isPresent());

        assertTrue(
                getDependency("pkg:npm/%40redhat/kogito-tooling-keyboard-shortcuts@0.9.0-2", bom.getDependencies())
                        .isPresent());
        assertTrue(
                getDependency(
                        "pkg:npm/%40redhat/kogito-tooling-keyboard-shortcuts@0.9.0-2",
                        dependency1.getDependencies()).isPresent());
        assertTrue(
                getDependency(
                        "pkg:npm/%40redhat/kogito-tooling-keyboard-shortcuts@0.9.0-2",
                        dependency2.getDependencies()).isPresent());
    }

    @Test
    void testSkipExistingComponent() throws IOException {
        // Mock PNC service
        PncService pncServiceMock = Mockito.mock(PncService.class);
        Build pncBuild = OBJECT_MAPPER.readValue(TestResources.asString("pnc/mavenBuild.json"), Build.class);
        List<Artifact> artifacts = OBJECT_MAPPER
                .readValue(TestResources.asString("pnc/npmDependencies.json"), new TypeReference<>() {
                });
        when(pncServiceMock.getBuild(("FOOBAR012345"))).thenReturn(pncBuild);
        when(pncServiceMock.getNPMDependencies(("FOOBAR012345"))).thenReturn(artifacts);

        // Prepare test component
        Bom bom = Objects.requireNonNull(SbomUtils.createBom());
        Component component1 = SbomUtils.createComponent(
                "foo.bar",
                "baz",
                "1.0.0.redhat-00001",
                "Test project",
                "pkg:maven/foo.bar/baz@1.0.0.redhat-00001?type=jar",
                Component.Type.LIBRARY);
        bom.addComponent(component1);
        bom.addDependency(SbomUtils.createDependency(component1.getBomRef()));
        SbomUtils.setPncBuildMetadata(component1, pncBuild, "pnc.example.com");

        Component component2 = SbomUtils
                .createComponent(null, "once", "1.4.0", null, "pkg:npm/once@1.4.0", Component.Type.LIBRARY);
        bom.addComponent(component2);
        bom.addDependency(SbomUtils.createDependency(component2.getBomRef()));

        // Check assertions before test
        assertEquals(2, bom.getComponents().size());
        assertEquals(2, bom.getDependencies().size());

        // Run test
        WorkaroundMissingNpmDependencies workaround = new WorkaroundMissingNpmDependencies(pncServiceMock);
        workaround.analyzeComponentsBuild(component1);
        workaround.analyzeComponentsBuild(component2);
        workaround.addMissingDependencies(bom);

        // Verify after test
        assertEquals(3, bom.getComponents().size());
        assertEquals(3, bom.getDependencies().size());

        Dependency dependency1 = getDependency(component1.getBomRef(), bom.getDependencies()).orElseThrow();
        Dependency dependency2 = getDependency(component2.getBomRef(), bom.getDependencies()).orElseThrow();
        assertEquals(1, dependency1.getDependencies().size());
        assertNull(dependency2.getDependencies());

        assertTrue(
                getDependency("pkg:npm/%40redhat/kogito-tooling-keyboard-shortcuts@0.9.0-2", bom.getDependencies())
                        .isPresent());
        assertTrue(
                getDependency(
                        "pkg:npm/%40redhat/kogito-tooling-keyboard-shortcuts@0.9.0-2",
                        dependency1.getDependencies()).isPresent());

        verify(pncServiceMock, times(1)).getNPMDependencies(("FOOBAR012345"));
    }

    @Test
    void testOverlappingBuilds() throws IOException {
        // Mock PNC service
        PncService pncServiceMock = Mockito.mock(PncService.class);
        Build pncBuild1 = OBJECT_MAPPER.readValue(TestResources.asString("pnc/mavenBuild.json"), Build.class);
        List<Artifact> artifacts1 = OBJECT_MAPPER
                .readValue(TestResources.asString("pnc/npmDependencies.json"), new TypeReference<>() {
                });
        Build pncBuild2 = OBJECT_MAPPER.readValue(TestResources.asString("pnc/mavenBuild2.json"), Build.class);
        List<Artifact> artifacts2 = OBJECT_MAPPER
                .readValue(TestResources.asString("pnc/npmDependencies2.json"), new TypeReference<>() {
                });
        when(pncServiceMock.getBuild(("FOOBAR012345"))).thenReturn(pncBuild1);
        when(pncServiceMock.getNPMDependencies(("FOOBAR012345"))).thenReturn(artifacts1);
        when(pncServiceMock.getBuild(("FOOBAZ012345"))).thenReturn(pncBuild2);
        when(pncServiceMock.getNPMDependencies(("FOOBAZ012345"))).thenReturn(artifacts2);

        // Prepare test component
        Bom bom = Objects.requireNonNull(SbomUtils.createBom());
        Component component1 = SbomUtils.createComponent(
                "foo.bar",
                "baz",
                "1.0.0.redhat-00001",
                "Test project",
                "pkg:maven/foo.bar/baz@1.0.0.redhat-00001?type=jar",
                Component.Type.LIBRARY);
        bom.addComponent(component1);
        bom.addDependency(SbomUtils.createDependency(component1.getBomRef()));
        SbomUtils.setPncBuildMetadata(component1, pncBuild1, "pnc.example.com");
        Component component2 = SbomUtils.createComponent(
                "foo.bar",
                "qux",
                "1.0.0.redhat-00001",
                "Test project",
                "pkg:maven/foo.bar/qux@1.0.0.redhat-00001?type=jar",
                Component.Type.LIBRARY);
        bom.addComponent(component2);
        bom.addDependency(SbomUtils.createDependency(component2.getBomRef()));
        SbomUtils.setPncBuildMetadata(component2, pncBuild2, "pnc.example.com");

        // Check assertions before test
        assertEquals(2, bom.getComponents().size());
        assertEquals(2, bom.getDependencies().size());

        // Run test
        WorkaroundMissingNpmDependencies workaround = new WorkaroundMissingNpmDependencies(pncServiceMock);
        workaround.analyzeComponentsBuild(component1);
        workaround.analyzeComponentsBuild(component2);
        workaround.addMissingDependencies(bom);

        // Verify after test
        assertEquals(5, bom.getComponents().size());
        assertEquals(5, bom.getDependencies().size());

        Dependency dependency1 = getDependency(component1.getBomRef(), bom.getDependencies()).orElseThrow();
        Dependency dependency2 = getDependency(component2.getBomRef(), bom.getDependencies()).orElseThrow();
        assertEquals(2, dependency1.getDependencies().size());
        assertEquals(2, dependency2.getDependencies().size());
        assertNotEquals(dependency1.getDependencies(), dependency2.getDependencies());

        assertTrue(getDependency("pkg:npm/once@1.4.0", bom.getDependencies()).isPresent());
        assertTrue(getDependency("pkg:npm/once@1.4.0", dependency1.getDependencies()).isPresent());
        assertTrue(getDependency("pkg:npm/once@1.4.0", dependency2.getDependencies()).isPresent());

        assertTrue(
                getDependency("pkg:npm/%40redhat/kogito-tooling-keyboard-shortcuts@0.9.0-2", bom.getDependencies())
                        .isPresent());
        assertTrue(
                getDependency(
                        "pkg:npm/%40redhat/kogito-tooling-keyboard-shortcuts@0.9.0-2",
                        dependency1.getDependencies()).isPresent());

        assertTrue(getDependency("pkg:npm/twice@1.4.0", bom.getDependencies()).isPresent());
        assertTrue(getDependency("pkg:npm/twice@1.4.0", dependency2.getDependencies()).isPresent());
    }

    private static Optional<Dependency> getDependency(String ref, List<Dependency> dependencies) {
        return dependencies.stream().filter(d -> d.getRef().equals(ref)).findFirst();
    }
}
