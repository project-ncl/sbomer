package org.jboss.sbomer.service.test.unit.generator.rhrelease;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.eclipse.microprofile.context.ManagedExecutor;
import org.jboss.sbomer.service.nextgen.core.dto.api.GenerationRequest;
import org.jboss.sbomer.service.nextgen.core.dto.api.Target;
import org.jboss.sbomer.service.nextgen.core.dto.model.EventRecord;
import org.jboss.sbomer.service.nextgen.core.dto.model.GenerationRecord;
import org.jboss.sbomer.service.nextgen.core.dto.model.ManifestRecord;
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

    private RedHatReleaseGenerator generator;

    @BeforeEach
    void beforeEach() {
        generator = new RedHatReleaseGenerator(client, managedExecutor);
    }

    private EventRecord genEvent(int numGenerations, int numManifests) {
        List<GenerationRecord> generations = new ArrayList<>();

        for (int i = 0; i < numGenerations; i++) {
            List<ManifestRecord> manifests = new ArrayList<>();

            for (int j = 0; j < numManifests; j++) {
                manifests.add(new ManifestRecord("M" + j + "G" + i, Instant.now()));
            }

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
                            manifests,
                            null,
                            null,
                            null));
        }

        return new EventRecord("E1", null, Instant.now(), Instant.now(), null, null, null, generations, null, null);
    }

    @Test
    void ensureSupportedTypes() {
        assertEquals(Set.of("EVENT"), generator.getSupportedTypes());
    }

    @Test
    void handleGeneration() {
        when(client.getEvent("E1MMM")).thenReturn(genEvent(2, 2));

        GenerationRecord generationRecord = new GenerationRecord(
                "G1",
                Instant.now(),
                Instant.now(),
                null,
                JacksonUtils.toObjectNode(new GenerationRequest(null, new Target("EVENT", "E1MMM"))),
                null,
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
        verify(client, times(1)).getEvent(anyString());
    }
}
