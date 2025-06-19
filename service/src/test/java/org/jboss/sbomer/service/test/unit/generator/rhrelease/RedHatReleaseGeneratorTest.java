package org.jboss.sbomer.service.test.unit.generator.rhrelease;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.eclipse.microprofile.context.ManagedExecutor;
import org.jboss.sbomer.service.nextgen.core.dto.api.GenerationRequest;
import org.jboss.sbomer.service.nextgen.core.dto.api.Generator;
import org.jboss.sbomer.service.nextgen.core.dto.api.Target;
import org.jboss.sbomer.service.nextgen.core.dto.model.GenerationRecord;
import org.jboss.sbomer.service.nextgen.core.dto.model.ManifestRecord;
import org.jboss.sbomer.service.nextgen.core.enums.GenerationStatus;
import org.jboss.sbomer.service.nextgen.core.events.GenerationStatusChangeEvent;
import org.jboss.sbomer.service.nextgen.core.rest.SBOMerClient;
import org.jboss.sbomer.service.nextgen.core.utils.JacksonUtils;
import org.jboss.sbomer.service.nextgen.generator.rhrelease.RedHatReleaseGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.JsonNode;

@ExtendWith(MockitoExtension.class)
public class RedHatReleaseGeneratorTest {

    @Mock
    ManagedExecutor managedExecutor;

    @Mock
    SBOMerClient client;

    RedHatReleaseGenerator generator;

    @BeforeEach
    void beforeEach() {
        generator = spy(new RedHatReleaseGenerator(client, managedExecutor));
    }

    private List<ManifestRecord> genManifests(int numManifests) {
        List<ManifestRecord> manifests = new ArrayList<>();

        for (int i = 0; i < numManifests; i++) {
            manifests.add(new ManifestRecord("M" + i, Instant.now()));
        }
        return manifests;
    }

    private List<GenerationRecord> genGenerations(int numGenerations) {
        List<GenerationRecord> generations = new ArrayList<>();

        for (int i = 0; i < numGenerations; i++) {

            generations.add(
                    new GenerationRecord(
                            "G" + i,
                            Instant.now(),
                            Instant.now(),
                            null,
                            JacksonUtils.toObjectNode(
                                    new GenerationRequest(
                                            null,
                                            new Target("CONTAINER_IMAGE", "quay.io/org/image1:tag"))),
                            null,
                            null,
                            null,
                            null));
        }

        return generations;
    }

    @Test
    void ensureSupportedTypes() {
        assertEquals(Set.of("EVENT"), generator.getSupportedTypes());
    }

    @Test
    void ensureGeneratorName() {
        assertEquals("redhat-release", generator.getGeneratorName());
    }

    @Test
    void ensureGeneratorVersion() {
        assertNotNull(generator.getGeneratorVersion());
        assertFalse(generator.getGeneratorVersion().isBlank());
    }

    @Test
    void shouldSkipEventIfUnsupportedGeneratorName() {
        GenerationStatusChangeEvent event = new GenerationStatusChangeEvent(
                new GenerationRecord(
                        "G",
                        Instant.now(),
                        Instant.now(),
                        null,
                        JacksonUtils.toObjectNode(
                                new GenerationRequest(
                                        new Generator("unsupported", "1.0", null),
                                        new Target("EVENT", "E1"))),
                        null,
                        GenerationStatus.SCHEDULED,
                        null,
                        null));

        generator.onEvent(event);

        verify(generator, times(0)).generate(any());
    }

    @Test
    void shouldSkipEventIfNoGeneratorIsProvided() {
        GenerationStatusChangeEvent event = new GenerationStatusChangeEvent(
                new GenerationRecord(
                        "G",
                        Instant.now(),
                        Instant.now(),
                        null,
                        JacksonUtils.toObjectNode(new GenerationRequest(null, new Target("EVENT", "E1"))),
                        null,
                        GenerationStatus.SCHEDULED,
                        null,
                        null));

        generator.onEvent(event);

        verify(generator, times(0)).generate(any());
    }

    @Test
    void shouldSkipEventIfNoRequestIsProvided() {
        GenerationStatusChangeEvent event = new GenerationStatusChangeEvent(
                new GenerationRecord(
                        "G",
                        Instant.now(),
                        Instant.now(),
                        null,
                        null,
                        null,
                        GenerationStatus.SCHEDULED,
                        null,
                        null));

        generator.onEvent(event);

        verify(generator, times(0)).generate(any());
    }

    @Test
    void shouldSkipEventIfUnsupportedGeneratorVersion() {
        GenerationStatusChangeEvent event = new GenerationStatusChangeEvent(
                new GenerationRecord(
                        "G",
                        Instant.now(),
                        Instant.now(),
                        null,
                        JacksonUtils.toObjectNode(
                                new GenerationRequest(
                                        new Generator("redhat-release", "1.0", null),
                                        new Target("EVENT", "E1"))),
                        null,
                        GenerationStatus.SCHEDULED,
                        null,
                        null));

        generator.onEvent(event);

        verify(generator, times(0)).generate(any());
    }

    @Test
    void handleGeneration() {
        when(client.getEventGenerations(eq("E1MMM"))).thenReturn(genGenerations(2));
        when(client.getGenerationManifests(eq("G0"))).thenReturn(genManifests(2));
        when(client.getGenerationManifests(eq("G1"))).thenReturn(genManifests(2));

        GenerationRecord generationRecord = new GenerationRecord(
                "G1",
                Instant.now(),
                Instant.now(),
                null,
                JacksonUtils.toObjectNode(new GenerationRequest(null, new Target("EVENT", "E1MMM"))),
                null,
                null,
                null,
                null);

        ArgumentCaptor<JsonNode> argumentCaptor = ArgumentCaptor.forClass(JsonNode.class);

        when(client.uploadManifest(eq("G1"), argumentCaptor.capture()))
                .thenReturn(new ManifestRecord("MX", Instant.now()));

        generator.generate(generationRecord);

        List<JsonNode> boms = argumentCaptor.getAllValues();

        assertEquals(5, boms.size()); // 4 updated manifests + 1 release manifest

        verify(client, times(4)).getManifestContent(anyString());
        verify(client, times(5)).uploadManifest(anyString(), any());
        verify(client, times(1)).getEventGenerations(anyString());
        verify(client, times(2)).getGenerationManifests(anyString());
    }
}
