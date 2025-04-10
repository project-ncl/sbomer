package org.jboss.sbomer.service.test.unit.feature.sbom.atlas;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.jboss.sbomer.core.errors.ApplicationException;
import org.jboss.sbomer.core.errors.ClientException;
import org.jboss.sbomer.core.test.TestResources;
import org.jboss.sbomer.service.feature.FeatureFlags;
import org.jboss.sbomer.service.feature.sbom.atlas.AtlasBuildClient;
import org.jboss.sbomer.service.feature.sbom.atlas.AtlasHandler;
import org.jboss.sbomer.service.feature.sbom.model.Sbom;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

class AtlasHandlerTest {
    static class AtlasHandlerAlt extends AtlasHandler {
        @Override
        public void uploadBuildManifest(Sbom sbom) {
            super.uploadBuildManifest(sbom);
        }
    }

    AtlasHandlerAlt atlasHandler;

    final AtlasBuildClient atlasBuildClient = mock(AtlasBuildClient.class);

    private Sbom generateSbom(String id, String purl) throws IOException {
        return generateSbom(id, purl, "sboms/complete_operation_sbom.json");
    }

    private Sbom generateSbom(String id, String purl, String path) throws IOException {
        Sbom sbom = new Sbom();
        sbom.setId(id);
        sbom.setRootPurl(purl);

        String bomJson = TestResources.asString(path);
        sbom.setSbom(new ObjectMapper().readTree(bomJson));

        return sbom;
    }

    @BeforeEach
    void beforeEach() {
        atlasHandler = new AtlasHandlerAlt();

        FeatureFlags featureFlags = mock(FeatureFlags.class);
        when(featureFlags.atlasPublish()).thenReturn(true);
        atlasHandler.setFeatureFlags(featureFlags);

        atlasHandler.setAtlasBuildClient(atlasBuildClient);
    }

    @Test
    void testUploadNothing() {
        // Should not fail, just a warning should be added
        assertDoesNotThrow(() -> atlasHandler.publishBuildManifests(null));
    }

    @Test
    void testUploadEmpty() {
        // Should not fail, just a warning should be added
        assertDoesNotThrow(() -> atlasHandler.publishBuildManifests(Collections.emptyList()));
    }

    @Test
    void testUploadBoms() throws Exception {
        Sbom sbomA = generateSbom("AAA", "pkg:maven/compA@1.1.0?type=pom");
        Sbom sbomB = generateSbom("BBB", "pkg:maven/compB@1.1.0?type=pom");

        atlasHandler.publishBuildManifests(List.of(sbomA, sbomB));

        verify(atlasBuildClient, times(1)).upload(eq("pkg:maven/compA@1.1.0?type=pom"), any(JsonNode.class));
        verify(atlasBuildClient, times(1)).upload(eq("pkg:maven/compB@1.1.0?type=pom"), any(JsonNode.class));
    }

    @Test
    void testHandlingOfApiErrors() throws Exception {
        Sbom sbom = generateSbom("AAA", "pkg:maven/compA@1.1.0?type=pom");

        doThrow(new ClientException("A reason")).when(atlasBuildClient).upload(anyString(), any(JsonNode.class));

        ApplicationException ex = assertThrows(
                ApplicationException.class,
                () -> atlasHandler.publishBuildManifests(List.of(sbom)));

        verify(atlasBuildClient, times(1)).upload(eq("pkg:maven/compA@1.1.0?type=pom"), any(JsonNode.class));

        assertEquals(
                "Unable to store 'AAA' manifest in Atlas, purl: 'pkg:maven/compA@1.1.0?type=pom': A reason",
                ex.getMessage());
    }
}
