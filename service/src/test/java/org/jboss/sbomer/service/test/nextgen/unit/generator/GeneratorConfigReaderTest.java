package org.jboss.sbomer.service.test.nextgen.unit.generator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.jboss.sbomer.core.errors.ClientException;
import org.jboss.sbomer.core.test.TestResources;
import org.jboss.sbomer.service.nextgen.core.payloads.generation.GenerationRequestSpec;
import org.jboss.sbomer.service.nextgen.core.payloads.generation.GeneratorVersionConfigSpec;
import org.jboss.sbomer.service.nextgen.core.payloads.generation.TargetSpec;
import org.jboss.sbomer.service.nextgen.service.config.GeneratorConfigProvider;
import org.jboss.sbomer.service.nextgen.service.config.mapping.GeneratorsConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.ConfigMapList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;

public class GeneratorConfigReaderTest {

    private static final String CM_NAME = "sbomer-generators-config";

    GeneratorConfigProvider generatorConfigProvider;

    KubernetesClient kubernetesClientMock;
    MixedOperation<ConfigMap, ConfigMapList, Resource<ConfigMap>> mockedConfigMapOps;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void beforeEach() {
        mockedConfigMapOps = mock(MixedOperation.class, RETURNS_DEEP_STUBS);
        kubernetesClientMock = mock(KubernetesClient.class, RETURNS_DEEP_STUBS);

        when(kubernetesClientMock.configMaps()).thenReturn(mockedConfigMapOps);

        generatorConfigProvider = new GeneratorConfigProvider(kubernetesClientMock);
    }

    @Test
    void shouldHandleNonExistingCm() {
        assertNull(generatorConfigProvider.getGeneratorsConfig());
    }

    @Test
    void shouldHandleCmWithMissingContent() {
        ConfigMap expectedConfigMap = new ConfigMapBuilder().withNewMetadata().withName(CM_NAME).endMetadata().build();

        when(kubernetesClientMock.configMaps().withName(CM_NAME).get()).thenReturn(expectedConfigMap);
        assertNull(generatorConfigProvider.getGeneratorsConfig());
    }

    @Test
    void shouldHandleCmWithProperContent() throws IOException {
        ConfigMap expectedConfigMap = new ConfigMapBuilder().withNewMetadata()
                .withName(CM_NAME)
                .endMetadata()
                .addToData("generators-config.yaml", TestResources.asString("generator/syft-only.yaml"))
                .build();

        when(kubernetesClientMock.configMaps().withName(CM_NAME).get()).thenReturn(expectedConfigMap);

        GeneratorsConfig config = generatorConfigProvider.getGeneratorsConfig();
        assertNotNull(config);
        assertTrue(config.defaultGeneratorMappings().get(0).targetType().equals("CONTAINER_IMAGE"));
        assertTrue(config.generatorProfiles().get(0).name().equals("syft"));
    }

    @Test
    void shouldHandleCaseWhenNoRequestIsProvided() {
        assertThrows(ClientException.class, () -> {
            generatorConfigProvider.buildEffectiveConfig(null);
        });
    }

    @Test
    void shouldCreateEffectiveConfigWhenNoConfigIsProvided() throws IOException {
        ConfigMap expectedConfigMap = new ConfigMapBuilder().withNewMetadata()
                .withName(CM_NAME)
                .endMetadata()
                .addToData("generators-config.yaml", TestResources.asString("generator/syft-only.yaml"))
                .build();

        when(kubernetesClientMock.configMaps().withName(CM_NAME).get()).thenReturn(expectedConfigMap);

        GeneratorVersionConfigSpec effectiveConfig = generatorConfigProvider
                .buildEffectiveConfig(new GenerationRequestSpec(new TargetSpec("image", "CONTAINER_IMAGE"), null));

        assertEquals("syft", effectiveConfig.name());
        assertEquals("1.26.1", effectiveConfig.version());
        assertEquals("CYCLONEDX_1.6_JSON", effectiveConfig.config().format());
        assertEquals("500m", effectiveConfig.config().resources().requests().cpu());
        assertEquals("1Gi", effectiveConfig.config().resources().requests().memory());
        assertEquals("1500m", effectiveConfig.config().resources().limits().cpu());
        assertEquals("3Gi", effectiveConfig.config().resources().limits().memory());
    }
}
