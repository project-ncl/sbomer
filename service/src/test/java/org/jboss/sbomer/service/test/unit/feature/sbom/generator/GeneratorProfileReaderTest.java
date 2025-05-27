package org.jboss.sbomer.service.test.unit.feature.sbom.generator;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.jboss.sbomer.core.test.TestResources;
import org.jboss.sbomer.service.v1beta2.generator.GeneratorProfileReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.ConfigMapList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;

public class GeneratorProfileReaderTest {

    GeneratorProfileReader generatorProfileReader;

    KubernetesClient kubernetesClientMock;
    MixedOperation<ConfigMap, ConfigMapList, Resource<ConfigMap>> mockedConfigMapOps;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void beforeEach() {
        mockedConfigMapOps = mock(MixedOperation.class, RETURNS_DEEP_STUBS);
        kubernetesClientMock = mock(KubernetesClient.class, RETURNS_DEEP_STUBS);

        when(kubernetesClientMock.configMaps()).thenReturn(mockedConfigMapOps);

        generatorProfileReader = new GeneratorProfileReader(kubernetesClientMock);
    }

    @Test
    void shouldHandleNonExistingCm() {
        assertNull(generatorProfileReader.getProfiles());
    }

    @Test
    void shouldHandleCmWithMissingContent() {
        ConfigMap expectedConfigMap = new ConfigMapBuilder().withNewMetadata()
                .withName("sbomer-generator-profiles")
                .endMetadata()
                .build();

        when(kubernetesClientMock.configMaps().withName("sbomer-generator-profiles").get())
                .thenReturn(expectedConfigMap);
        assertNull(generatorProfileReader.getProfiles());
    }

    @Test
    void shouldHandleCmWithProperContent() throws IOException {
        ConfigMap expectedConfigMap = new ConfigMapBuilder().withNewMetadata()
                .withName("sbomer-generator-profiles")
                .endMetadata()
                .addToData("generatorProfiles", TestResources.asString("generator-profiles/minimal.yaml"))
                .build();

        System.out.println(TestResources.asString("generator-profiles/syft.yaml"));

        when(kubernetesClientMock.configMaps().withName("sbomer-generator-profiles").get())
                .thenReturn(expectedConfigMap);
        assertNotNull(generatorProfileReader.getProfiles());
    }
}
