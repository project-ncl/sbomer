package org.jboss.sbomer.service.test.unit.feature.sbom.reconciler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.sbomer.core.features.sbom.enums.GenerationRequestType;
import org.jboss.sbomer.service.feature.sbom.k8s.reconciler.TektonResourceUtils;
import org.jboss.sbomer.service.feature.sbom.k8s.resources.Labels;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.ResourceRequirements;
import io.fabric8.tekton.v1beta1.TaskRun;
import io.fabric8.tekton.v1beta1.TaskRunBuilder;

class TektonResourceUtilsTest {

    @Test
    void shouldHandleContainerImageResources() {
        TaskRun taskRun = new TaskRunBuilder().withNewMetadata()
                .withName("taskrun-name")
                .withLabels(Map.of(Labels.LABEL_GENERATION_REQUEST_TYPE, GenerationRequestType.CONTAINERIMAGE.toName()))
                .endMetadata()
                .withNewSpec()
                .endSpec()
                .build();

        TektonResourceUtils.adjustComputeResources(taskRun);

        ResourceRequirements computeResources = taskRun.getSpec().getComputeResources();

        // CPU
        assertEquals(Quantity.parse("800m"), computeResources.getRequests().get("cpu"));
        assertEquals(Quantity.parse("1000m"), computeResources.getLimits().get("cpu"));

        // Memory
        assertEquals(Quantity.parse("1200Mi"), computeResources.getRequests().get("memory"));
        assertEquals(Quantity.parse("1400Mi"), computeResources.getLimits().get("memory"));
    }

    @Test
    void shouldSetMockedResources() {
        Config mockConfig = mock(Config.class);

        when(mockConfig.getOptionalValue("sbomer.generator.containerimage.tekton.resources.requests.cpu", String.class))
                .thenReturn(Optional.of("100m"));

        when(mockConfig.getOptionalValue("sbomer.generator.containerimage.tekton.resources.limits.cpu", String.class))
                .thenReturn(Optional.of("200m"));

        when(
                mockConfig.getOptionalValue(
                        "sbomer.generator.containerimage.tekton.resources.requests.memory",
                        String.class))
                .thenReturn(Optional.of("100Mi"));

        when(
                mockConfig.getOptionalValue(
                        "sbomer.generator.containerimage.tekton.resources.limits.memory",
                        String.class))
                .thenReturn(Optional.of("200Mi"));

        TaskRun taskRun = new TaskRunBuilder().withNewMetadata()
                .withName("taskrun-name")
                .withLabels(Map.of(Labels.LABEL_GENERATION_REQUEST_TYPE, GenerationRequestType.CONTAINERIMAGE.toName()))
                .endMetadata()
                .withNewSpec()
                .endSpec()
                .build();

        try (MockedStatic<ConfigProvider> utilities = Mockito.mockStatic(ConfigProvider.class)) {
            utilities.when(ConfigProvider::getConfig).thenReturn(mockConfig);

            TektonResourceUtils.adjustComputeResources(taskRun);
        }

        ResourceRequirements computeResources = taskRun.getSpec().getComputeResources();

        // CPU
        assertEquals(Quantity.parse("100m"), computeResources.getRequests().get("cpu"));
        assertEquals(Quantity.parse("200m"), computeResources.getLimits().get("cpu"));

        // Memoery
        assertEquals(Quantity.parse("100Mi"), computeResources.getRequests().get("memory"));
        assertEquals(Quantity.parse("200Mi"), computeResources.getLimits().get("memory"));
    }

    @Test
    void shouldNotSetResourcesInCaseOfFailure() {
        Config mockConfig = mock(Config.class);

        when(mockConfig.getOptionalValue("sbomer.generator.containerimage.tekton.resources.requests.cpu", String.class))
                .thenReturn(Optional.of("100m"));

        when(mockConfig.getOptionalValue("sbomer.generator.containerimage.tekton.resources.limits.cpu", String.class))
                .thenReturn(Optional.of("200m"));

        when(
                mockConfig.getOptionalValue(
                        "sbomer.generator.containerimage.tekton.resources.requests.memory",
                        String.class))
                .thenReturn(Optional.of("100Mi"));

        // This is the failing one
        when(
                mockConfig.getOptionalValue(
                        "sbomer.generator.containerimage.tekton.resources.limits.memory",
                        String.class))
                .thenReturn(Optional.of("2garbage00Mi"));

        TaskRun taskRun = new TaskRunBuilder().withNewMetadata()
                .withName("taskrun-name")
                .withLabels(Map.of(Labels.LABEL_GENERATION_REQUEST_TYPE, GenerationRequestType.CONTAINERIMAGE.toName()))
                .endMetadata()
                .withNewSpec()
                .endSpec()
                .build();

        try (MockedStatic<ConfigProvider> utilities = Mockito.mockStatic(ConfigProvider.class)) {
            utilities.when(ConfigProvider::getConfig).thenReturn(mockConfig);

            TektonResourceUtils.adjustComputeResources(taskRun);
        }

        ResourceRequirements computeResources = taskRun.getSpec().getComputeResources();

        assertNull(computeResources);
    }
}
