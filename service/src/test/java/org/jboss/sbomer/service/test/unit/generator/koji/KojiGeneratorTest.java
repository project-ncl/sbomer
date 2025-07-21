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
package org.jboss.sbomer.service.test.unit.generator.koji;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.Instant;
import java.util.Set;

import org.eclipse.microprofile.context.ManagedExecutor;
import org.jboss.sbomer.service.feature.sbom.config.GenerationRequestControllerConfig;
import org.jboss.sbomer.service.leader.LeaderManager;
import org.jboss.sbomer.service.nextgen.core.dto.api.GenerationRequest;
import org.jboss.sbomer.service.nextgen.core.dto.api.Generator;
import org.jboss.sbomer.service.nextgen.core.dto.api.Target;
import org.jboss.sbomer.service.nextgen.core.dto.model.GenerationRecord;
import org.jboss.sbomer.service.nextgen.core.enums.GenerationStatus;
import org.jboss.sbomer.service.nextgen.core.events.GenerationStatusChangeEvent;
import org.jboss.sbomer.service.nextgen.core.rest.SBOMerClient;
import org.jboss.sbomer.service.nextgen.core.utils.JacksonUtils;
import org.jboss.sbomer.service.nextgen.generator.koji.KojiGenerator;
import org.jboss.sbomer.service.nextgen.service.EntityMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.fabric8.kubernetes.client.KubernetesClient;

@ExtendWith(MockitoExtension.class)
public class KojiGeneratorTest {

    @Mock
    ManagedExecutor managedExecutor;

    @Mock
    SBOMerClient client;

    @Mock
    KubernetesClient kubernetesClient;

    @Mock
    GenerationRequestControllerConfig controllerConfig;

    @Mock
    EntityMapper mapper;

    @Mock
    LeaderManager leaderManager;

    KojiGenerator generator;

    @BeforeEach
    void beforeEach() {
        generator = spy(
                new KojiGenerator(client, kubernetesClient, controllerConfig, managedExecutor, mapper, leaderManager));
    }

    @Test
    void ensureSupportedTypes() {
        assertEquals(Set.of("BREW_RPM"), generator.getSupportedTypes());
    }

    @Test
    void ensureGeneratorName() {
        assertEquals("koji", generator.getGeneratorName());
    }

    @Test
    void ensureGeneratorVersion() {
        assertNotNull(generator.getGeneratorVersion());
        assertFalse(generator.getGeneratorVersion().isBlank());
    }

    @Test
    void shouldInvokeGenerationEventForKojiBrewRPM() {
        GenerationStatusChangeEvent event = createGenerationStatusChangeEvent(
                "G",
                "koji",
                "0.1.0",
                "BREW_RPM",
                "TEST1");

        ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
        generator.onEvent(event);
        verify(managedExecutor).runAsync(runnableCaptor.capture());
        runnableCaptor.getValue().run();

        verify(generator, times(1)).generate(any());
    }

    @Test
    void shouldNotInvokeGenerationEventForEmptyGenerator() {
        GenerationStatusChangeEvent event = createGenerationStatusChangeEventWithoutGenerator("G", "BREW_RPM", "TEST1");
        generator.onEvent(event);
        verifyNoInteractions(managedExecutor);
        verify(generator, never()).generate(any());
    }

    @Test
    void shouldNotInvokeGenerationEventForDifferentGenerator() {
        GenerationStatusChangeEvent event = createGenerationStatusChangeEvent("G", "syft", "1.27", "BREW_RPM", "TEST1");
        generator.onEvent(event);
        verifyNoInteractions(managedExecutor);
        verify(generator, never()).generate(any());
    }

    @Test
    void shouldNotInvokeGenerationEventForDifferentType() {
        GenerationStatusChangeEvent event = createGenerationStatusChangeEvent(
                "G",
                "koji",
                "0.1.0",
                "CONTAINER_IMAGE",
                "TEST1");
        generator.onEvent(event);
        verifyNoInteractions(managedExecutor);
        verify(generator, never()).generate(any());
    }

    private GenerationStatusChangeEvent createGenerationStatusChangeEvent(
            String generationId,
            String generatorName,
            String generatorVersion,
            String typeName,
            String identifier) {
        return new GenerationStatusChangeEvent(
                new GenerationRecord(
                        generationId,
                        Instant.now(),
                        Instant.now(),
                        null,
                        JacksonUtils.toObjectNode(
                                new GenerationRequest(
                                        new Generator(generatorName, generatorVersion, null),
                                        new Target(typeName, identifier))),
                        null,
                        GenerationStatus.SCHEDULED,
                        null,
                        null));
    }

    private GenerationStatusChangeEvent createGenerationStatusChangeEventWithoutGenerator(
            String generationId,
            String typeName,
            String identifier) {
        return new GenerationStatusChangeEvent(
                new GenerationRecord(
                        generationId,
                        Instant.now(),
                        Instant.now(),
                        null,
                        JacksonUtils.toObjectNode(new GenerationRequest(null, new Target(typeName, identifier))),
                        null,
                        GenerationStatus.SCHEDULED,
                        null,
                        null));
    }

}