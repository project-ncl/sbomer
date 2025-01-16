package org.jboss.sbomer.cli.test.unit.feature.sbom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.cyclonedx.model.Bom;
import org.cyclonedx.model.Commit;
import org.cyclonedx.model.Component;
import org.cyclonedx.model.Dependency;
import org.cyclonedx.model.ExternalReference;
import org.cyclonedx.model.ExternalReference.Type;
import org.jboss.pnc.build.finder.core.BuildConfig;
import org.jboss.pnc.dto.Artifact;
import org.jboss.pnc.dto.Build;
import org.jboss.pnc.dto.BuildConfigurationRevisionRef;
import org.jboss.pnc.enums.BuildType;
import org.jboss.sbomer.cli.feature.sbom.processor.DefaultProcessor;
import org.jboss.sbomer.cli.feature.sbom.service.KojiService;
import org.jboss.sbomer.core.features.sbom.Constants;
import org.jboss.sbomer.core.features.sbom.utils.ObjectMapperProvider;
import org.jboss.sbomer.core.features.sbom.utils.SbomUtils;
import org.jboss.sbomer.core.pnc.PncService;
import org.jboss.sbomer.core.test.TestResources;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.redhat.red.build.koji.KojiClientException;
import com.redhat.red.build.koji.model.xmlrpc.KojiBuildInfo;

public class DefaultProcessorTest {

    private static final ObjectMapper OBJECT_MAPPER = ObjectMapperProvider.json();

    @Test
    void testProcessingWhenNoDataIsAvailable() throws IOException {
        PncService pncServiceMock = Mockito.mock(PncService.class);
        KojiService kojiServiceMock = Mockito.mock(KojiService.class);

        DefaultProcessor defaultProcessor = new DefaultProcessor(pncServiceMock, kojiServiceMock);

        Bom bom = SbomUtils.fromString(TestResources.asString("boms/image-after-adjustments.json"));
        Bom processed = defaultProcessor.process(bom);

        assertEquals(192, processed.getComponents().size());
    }

    @Test
    void testAddBrewInfo() throws IOException, KojiClientException {
        PncService pncServiceMock = Mockito.mock(PncService.class);
        KojiService kojiServiceMock = Mockito.mock(KojiService.class);

        KojiBuildInfo kojiBuildInfo = new KojiBuildInfo();
        kojiBuildInfo.setId(12345);
        kojiBuildInfo.setSource("https://git.com/repo#hash");

        BuildConfig buildConfig = new BuildConfig();
        buildConfig.setKojiWebURL(new URL("https://koji.web"));

        when(kojiServiceMock.getConfig()).thenReturn(buildConfig);
        when(kojiServiceMock.findBuild(eq("amqstreams-console-ui-container-2.7.0-8.1718294415")))
                .thenReturn(kojiBuildInfo);

        DefaultProcessor defaultProcessor = new DefaultProcessor(pncServiceMock, kojiServiceMock);

        Bom bom = SbomUtils.fromString(TestResources.asString("boms/image-after-adjustments.json"));
        Bom processed = defaultProcessor.process(bom);

        assertEquals(192, processed.getComponents().size());

        Component mainComponent = processed.getComponents().get(0);

        assertEquals("Red Hat", mainComponent.getSupplier().getName());
        assertEquals("Red Hat", mainComponent.getPublisher());

        ExternalReference buildSystem = SbomUtils.getExternalReferences(mainComponent, Type.BUILD_SYSTEM).get(0);

        assertEquals("https://koji.web/buildinfo?buildID=12345", buildSystem.getUrl());
        assertEquals("brew-build-id", buildSystem.getComment());

        assertEquals(
                "https://git.com/repo#hash",
                SbomUtils.getExternalReferences(mainComponent, Type.VCS).get(0).getUrl());

        Commit commit = mainComponent.getPedigree().getCommits().get(0);

        assertEquals("hash", commit.getUid());
        assertEquals("https://git.com/repo#hash", commit.getUrl());

    }

    @Test
    void testAddBrewInfoForRpm() throws IOException, KojiClientException {
        PncService pncServiceMock = Mockito.mock(PncService.class);
        KojiService kojiServiceMock = Mockito.mock(KojiService.class);

        KojiBuildInfo kojiBuildInfo = new KojiBuildInfo();
        kojiBuildInfo.setId(12345);
        kojiBuildInfo.setSource("https://git.com/repo#hash");

        BuildConfig buildConfig = new BuildConfig();
        buildConfig.setKojiWebURL(new URL("https://koji.web"));

        when(kojiServiceMock.getConfig()).thenReturn(buildConfig);
        when(kojiServiceMock.findBuildByRPM(eq("audit-libs-3.0.7-103.el9.x86_64"))).thenReturn(kojiBuildInfo);

        DefaultProcessor defaultProcessor = new DefaultProcessor(pncServiceMock, kojiServiceMock);

        Bom bom = SbomUtils.fromString(TestResources.asString("boms/image-after-adjustments.json"));
        Bom processed = defaultProcessor.process(bom);

        assertEquals(192, processed.getComponents().size());

        Component rpmComponent = getComponent(processed, "pkg:rpm/redhat/audit-libs@3.0.7-103.el9?arch=x86_64")
                .orElseThrow();

        assertNotNull(rpmComponent.getSupplier());
        assertEquals("Red Hat", rpmComponent.getSupplier().getName());
        assertEquals("Red Hat", rpmComponent.getPublisher());

        ExternalReference buildSystem = SbomUtils.getExternalReferences(rpmComponent, Type.BUILD_SYSTEM).get(0);

        assertEquals("https://koji.web/buildinfo?buildID=12345", buildSystem.getUrl());
        assertEquals("brew-build-id", buildSystem.getComment());

        assertEquals(
                "https://git.com/repo#hash",
                SbomUtils.getExternalReferences(rpmComponent, Type.VCS).get(0).getUrl());

        Commit commit = rpmComponent.getPedigree().getCommits().get(0);

        assertEquals("hash", commit.getUid());
        assertEquals("https://git.com/repo#hash", commit.getUrl());
    }

    @Test
    void testAddMissingNpmDependencies() throws IOException {
        DefaultProcessor defaultProcessor = mockForAddMissingNpmDependencies();

        // With
        Bom bom = SbomUtils.fromString(TestResources.asString("boms/pnc-build.json"));
        Optional<Component> missingComponent = getComponent(bom, "pkg:npm/once@1.4.0");
        Optional<Dependency> missingDependency = getDependency("pkg:npm/once@1.4.0", bom.getDependencies());

        assertTrue(missingComponent.isEmpty());
        assertTrue(missingDependency.isEmpty());
        assertEquals(8, bom.getComponents().size());

        // When
        Bom processed = defaultProcessor.process(bom);

        // Then
        assertEquals(10, processed.getComponents().size());
        verifyAddedNpmDependencies(processed);
    }

    private static DefaultProcessor mockForAddMissingNpmDependencies() throws IOException {
        PncService pncServiceMock = Mockito.mock(PncService.class);
        KojiService kojiServiceMock = Mockito.mock(KojiService.class);

        List<Artifact> artifacts = OBJECT_MAPPER
                .readValue(TestResources.asString("pnc/npmDependencies.json"), new TypeReference<>() {
                });

        Build build = Build.builder()
                .id("BALVSAEVTGYAY")
                .buildConfigRevision(BuildConfigurationRevisionRef.refBuilder().buildType(BuildType.MVN).build())
                .build();

        when(pncServiceMock.getApiUrl()).thenReturn("pnc.example.com");
        when(pncServiceMock.getBuild(eq("BALVSAEVTGYAY"))).thenReturn(build);
        when(pncServiceMock.getNPMDependencies(eq("BALVSAEVTGYAY"))).thenReturn(artifacts);

        return new DefaultProcessor(pncServiceMock, kojiServiceMock);
    }

    private static void verifyAddedNpmDependencies(Bom processed) {
        Dependency mainDependency = getDependency(
                "pkg:maven/org.keycloak/keycloak-parent@24.0.6.redhat-00001?type=pom",
                processed.getDependencies()).orElseThrow();

        Component componentOnce = getComponent(processed, "pkg:npm/once@1.4.0").orElseThrow();
        ExternalReference onceArtifact = SbomUtils
                .getExternalReferences(componentOnce, Type.BUILD_SYSTEM, Constants.SBOM_RED_HAT_PNC_ARTIFACT_ID)
                .get(0);
        assertEquals("https://pnc.example.com/pnc-rest/v2/artifacts/2160610", onceArtifact.getUrl());
        assertTrue(getDependency("pkg:npm/once@1.4.0", mainDependency.getDependencies()).isPresent());
        assertTrue(getDependency("pkg:npm/once@1.4.0", processed.getDependencies()).isPresent());
        assertNull(componentOnce.getGroup());
        assertEquals("once", componentOnce.getName());
        assertEquals("1.4.0", componentOnce.getVersion());

        Component componentKogito = getComponent(
                processed,
                "pkg:npm/%40redhat/kogito-tooling-keyboard-shortcuts@0.9.0-2").orElseThrow();
        assertNotNull(componentKogito.getSupplier());
        assertEquals("Red Hat", componentKogito.getSupplier().getName());
        assertEquals("Red Hat", componentKogito.getPublisher());
        ExternalReference buildSystem = SbomUtils
                .getExternalReferences(componentKogito, Type.BUILD_SYSTEM, Constants.SBOM_RED_HAT_PNC_BUILD_ID)
                .get(0);
        assertEquals("https://pnc.example.com/pnc-rest/v2/builds/96015", buildSystem.getUrl());
        assertEquals(
                "https://github.com/kiegroup/kogito-tooling.git",
                SbomUtils.getExternalReferences(componentKogito, Type.VCS).get(0).getUrl());
        Commit commit = componentKogito.getPedigree().getCommits().get(0);
        assertEquals("88c1a77824c7bf0b636f24b4bde2b87c0b38d8a1", commit.getUid());
        assertEquals("http://git.example.com/gerrit/kiegroup/kogito-tooling.git#0.9.0", commit.getUrl());
        assertTrue(
                getDependency(
                        "pkg:npm/%40redhat/kogito-tooling-keyboard-shortcuts@0.9.0-2",
                        mainDependency.getDependencies()).isPresent());
        assertTrue(
                getDependency(
                        "pkg:npm/%40redhat/kogito-tooling-keyboard-shortcuts@0.9.0-2",
                        processed.getDependencies()).isPresent());
        assertEquals("@redhat", componentKogito.getGroup());
        assertEquals("kogito-tooling-keyboard-shortcuts", componentKogito.getName());
        assertEquals("0.9.0-2", componentKogito.getVersion());
    }

    private static Optional<Dependency> getDependency(String ref, List<Dependency> dependencies) {
        return dependencies.stream().filter(d -> d.getRef().equals(ref)).findFirst();
    }

    private static Optional<Component> getComponent(Bom bom, String purl) {
        return bom.getComponents().stream().filter(c -> purl.equals(c.getPurl())).findFirst();
    }

    @Test
    void baseTest() throws IOException {
        Bom bom = SbomUtils.fromString(TestResources.asString("boms/adjusted.json"));

        PncService pncServiceMock = Mockito.mock(PncService.class);
        KojiService kojiServiceMock = Mockito.mock(KojiService.class);

        DefaultProcessor defaultProcessor = new DefaultProcessor(pncServiceMock, kojiServiceMock);

        assertEquals(459, bom.getComponents().size());
        assertEquals(459, bom.getDependencies().size());

        defaultProcessor.process(bom);

        assertEquals(459, bom.getComponents().size());
        assertEquals(459, bom.getDependencies().size());
    }
}
