package org.jboss.sbomer.cli.test.unit.feature.sbom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URL;

import org.cyclonedx.model.Bom;
import org.cyclonedx.model.Commit;
import org.cyclonedx.model.Component;
import org.cyclonedx.model.ExternalReference;
import org.cyclonedx.model.ExternalReference.Type;
import org.jboss.pnc.build.finder.core.BuildConfig;
import org.jboss.sbomer.cli.feature.sbom.processor.DefaultProcessor;
import org.jboss.sbomer.cli.feature.sbom.service.KojiService;
import org.jboss.sbomer.core.features.sbom.utils.SbomUtils;
import org.jboss.sbomer.core.pnc.PncService;
import org.jboss.sbomer.core.test.TestResources;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.redhat.red.build.koji.KojiClientException;
import com.redhat.red.build.koji.model.xmlrpc.KojiBuildInfo;

public class DefaultProcessorTest {

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
        when(kojiServiceMock.findBuildByRPM(eq("audit-libs-3.0.7-103.el9.x86_64")))
                .thenReturn(kojiBuildInfo);

        DefaultProcessor defaultProcessor = new DefaultProcessor(pncServiceMock, kojiServiceMock);

        Bom bom = SbomUtils.fromString(TestResources.asString("boms/image-after-adjustments.json"));
        Bom processed = defaultProcessor.process(bom);

        assertEquals(192, processed.getComponents().size());

        Component rpmComponent = processed.getComponents().stream()
                .filter(c -> "pkg:rpm/redhat/audit-libs@3.0.7-103.el9?arch=x86_64".equals(c.getPurl()))
                .findFirst().orElseThrow();

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
