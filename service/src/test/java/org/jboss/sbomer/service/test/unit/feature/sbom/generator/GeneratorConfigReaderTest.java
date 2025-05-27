package org.jboss.sbomer.service.test.unit.feature.sbom.generator;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.jboss.sbomer.core.test.TestResources;
import org.jboss.sbomer.service.v1beta2.generator.GeneratorConfigReader;
import org.jboss.sbomer.service.v1beta2.generator.GeneratorsConfig;
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

    GeneratorConfigReader generatorProfileReader;

    KubernetesClient kubernetesClientMock;
    MixedOperation<ConfigMap, ConfigMapList, Resource<ConfigMap>> mockedConfigMapOps;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void beforeEach() {
        mockedConfigMapOps = mock(MixedOperation.class, RETURNS_DEEP_STUBS);
        kubernetesClientMock = mock(KubernetesClient.class, RETURNS_DEEP_STUBS);

        when(kubernetesClientMock.configMaps()).thenReturn(mockedConfigMapOps);

        generatorProfileReader = new GeneratorConfigReader(kubernetesClientMock);
    }

    @Test
    void shouldHandleNonExistingCm() {
        assertNull(generatorProfileReader.getConfig());
    }

    @Test
    void shouldHandleCmWithMissingContent() {
        ConfigMap expectedConfigMap = new ConfigMapBuilder().withNewMetadata().withName(CM_NAME).endMetadata().build();

        when(kubernetesClientMock.configMaps().withName(CM_NAME).get()).thenReturn(expectedConfigMap);
        assertNull(generatorProfileReader.getConfig());
    }

    @Test
    void shouldHandleCmWithProperContent() throws IOException {
        ConfigMap expectedConfigMap = new ConfigMapBuilder().withNewMetadata()
                .withName(CM_NAME)
                .endMetadata()
                .addToData("generators-config.yaml", TestResources.asString("generator/syft-only.yaml"))
                .build();

        when(kubernetesClientMock.configMaps().withName(CM_NAME).get()).thenReturn(expectedConfigMap);

        GeneratorsConfig config = generatorProfileReader.getConfig();
        assertNotNull(config);
        assertTrue(config.defaultGeneratorMappings().get(0).targetType().equals("CONTAINER_IMAGE"));
        assertTrue(config.generatorProfiles().get(0).name().equals("syft"));
    }
}
