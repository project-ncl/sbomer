package org.jboss.sbomer.service.test.unit.nextgen.unit.generator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.jboss.sbomer.core.errors.ApplicationException;
import org.jboss.sbomer.core.errors.ClientException;
import org.jboss.sbomer.core.test.TestResources;
import org.jboss.sbomer.service.nextgen.core.payloads.generation.GenerationRequestSpec;
import org.jboss.sbomer.service.nextgen.core.payloads.generation.GeneratorConfigSpec;
import org.jboss.sbomer.service.nextgen.core.payloads.generation.GeneratorVersionConfigSpec;
import org.jboss.sbomer.service.nextgen.core.payloads.generation.ResourceRequirementSpec;
import org.jboss.sbomer.service.nextgen.core.payloads.generation.ResourcesSpec;
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

public class GeneratorConfigProviderTest {

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
        ApplicationException ex = assertThrows(ApplicationException.class, () -> {
            generatorConfigProvider.getConfig();
        });

        assertEquals(
                "Could not read generators config, please make sure the system is properly configured, unable to process request",
                ex.getMessage());

    }

    @Test
    void shouldHandleCmWithMissingContent() {
        ConfigMap expectedConfigMap = new ConfigMapBuilder().withNewMetadata().withName(CM_NAME).endMetadata().build();
        when(kubernetesClientMock.configMaps().withName(CM_NAME).get()).thenReturn(expectedConfigMap);

        ApplicationException ex = assertThrows(ApplicationException.class, () -> {
            generatorConfigProvider.getConfig();
        });

        assertEquals(
                "Could not read generators config, please make sure the system is properly configured, unable to process request",
                ex.getMessage());
    }

    @Test
    void shouldHandleCmWithProperContent() throws IOException {
        ConfigMap expectedConfigMap = new ConfigMapBuilder().withNewMetadata()
                .withName(CM_NAME)
                .endMetadata()
                .addToData("generators-config.yaml", TestResources.asString("generator/syft-only.yaml"))
                .build();

        when(kubernetesClientMock.configMaps().withName(CM_NAME).get()).thenReturn(expectedConfigMap);

        GeneratorsConfig config = generatorConfigProvider.getConfig();
        assertNotNull(config);
        assertTrue(config.defaultGeneratorMappings().get(0).targetType().equals("CONTAINER_IMAGE"));
        assertTrue(config.generatorProfiles().get(0).name().equals("syft"));
    }

    @Test
    void shouldHandleCaseWhenNoRequestIsProvided() {
        assertThrows(ClientException.class, () -> {
            generatorConfigProvider.buildEffectiveRequest(null);
        });
    }

    @Test
    void shouldCreateDefaultConfigWhenNoConfigIsProvided() throws IOException {
        ConfigMap expectedConfigMap = new ConfigMapBuilder().withNewMetadata()
                .withName(CM_NAME)
                .endMetadata()
                .addToData("generators-config.yaml", TestResources.asString("generator/syft-only.yaml"))
                .build();

        when(kubernetesClientMock.configMaps().withName(CM_NAME).get()).thenReturn(expectedConfigMap);

        GenerationRequestSpec requestSpec = generatorConfigProvider
                .buildEffectiveRequest(new GenerationRequestSpec(new TargetSpec("image", "CONTAINER_IMAGE"), null));

        assertEquals("syft", requestSpec.generator().name());
        assertEquals("1.16.0", requestSpec.generator().version());
        assertEquals("CYCLONEDX_1.6_JSON", requestSpec.generator().config().format());
        assertEquals("500m", requestSpec.generator().config().resources().requests().cpu());
        assertEquals("1Gi", requestSpec.generator().config().resources().requests().memory());
        assertEquals("1500m", requestSpec.generator().config().resources().limits().cpu());
        assertEquals("3Gi", requestSpec.generator().config().resources().limits().memory());
    }

    @Test
    void shouldAllowForOverridingResources() throws IOException {
        ConfigMap expectedConfigMap = new ConfigMapBuilder().withNewMetadata()
                .withName(CM_NAME)
                .endMetadata()
                .addToData("generators-config.yaml", TestResources.asString("generator/syft-only.yaml"))
                .build();

        when(kubernetesClientMock.configMaps().withName(CM_NAME).get()).thenReturn(expectedConfigMap);

        GenerationRequestSpec requestSpec = generatorConfigProvider.buildEffectiveRequest(
                new GenerationRequestSpec(
                        new TargetSpec("image", "CONTAINER_IMAGE"),
                        new GeneratorVersionConfigSpec(
                                "syft",
                                null,
                                new GeneratorConfigSpec(
                                        null,
                                        new ResourcesSpec(
                                                new ResourceRequirementSpec("800m", "2Gi"),
                                                new ResourceRequirementSpec("2000m", "4Gi")),
                                        null))));

        assertEquals("syft", requestSpec.generator().name());
        assertEquals("1.16.0", requestSpec.generator().version());
        assertEquals("CYCLONEDX_1.6_JSON", requestSpec.generator().config().format());
        assertEquals("800m", requestSpec.generator().config().resources().requests().cpu());
        assertEquals("2Gi", requestSpec.generator().config().resources().requests().memory());
        assertEquals("2000m", requestSpec.generator().config().resources().limits().cpu());
        assertEquals("4Gi", requestSpec.generator().config().resources().limits().memory());

    }

    @Test
    void shouldAllowForPartialOverridingOfResources() throws IOException {
        ConfigMap expectedConfigMap = new ConfigMapBuilder().withNewMetadata()
                .withName(CM_NAME)
                .endMetadata()
                .addToData("generators-config.yaml", TestResources.asString("generator/syft-only.yaml"))
                .build();

        when(kubernetesClientMock.configMaps().withName(CM_NAME).get()).thenReturn(expectedConfigMap);

        GenerationRequestSpec requestSpec = generatorConfigProvider.buildEffectiveRequest(
                new GenerationRequestSpec(
                        new TargetSpec("image", "CONTAINER_IMAGE"),
                        new GeneratorVersionConfigSpec(
                                "syft",
                                null,
                                new GeneratorConfigSpec(
                                        null,
                                        new ResourcesSpec(null, new ResourceRequirementSpec("3000m", "4Gi")),
                                        null))));

        assertEquals("syft", requestSpec.generator().name());
        assertEquals("1.16.0", requestSpec.generator().version());
        assertEquals("CYCLONEDX_1.6_JSON", requestSpec.generator().config().format());
        assertEquals("500m", requestSpec.generator().config().resources().requests().cpu());
        assertEquals("1Gi", requestSpec.generator().config().resources().requests().memory());
        assertEquals("3000m", requestSpec.generator().config().resources().limits().cpu());
        assertEquals("4Gi", requestSpec.generator().config().resources().limits().memory());
    }
}
