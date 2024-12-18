package org.jboss.sbomer.service.test.unit.feature.sbom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.jboss.sbomer.core.patch.cyclonedx.model.Bom;
import org.jboss.sbomer.core.features.sbom.config.Config;
import org.jboss.sbomer.core.features.sbom.config.PncBuildConfig;
import org.jboss.sbomer.core.features.sbom.enums.GenerationRequestType;
import org.jboss.sbomer.core.features.sbom.enums.GenerationResult;
import org.jboss.sbomer.core.features.sbom.utils.SbomUtils;
import org.jboss.sbomer.core.test.TestResources;
import org.jboss.sbomer.service.feature.sbom.atlas.AtlasHandler;
import org.jboss.sbomer.service.feature.sbom.config.GenerationRequestControllerConfig;
import org.jboss.sbomer.service.feature.sbom.features.umb.producer.NotificationService;
import org.jboss.sbomer.service.feature.sbom.k8s.model.GenerationRequest;
import org.jboss.sbomer.service.feature.sbom.k8s.model.GenerationRequestBuilder;
import org.jboss.sbomer.service.feature.sbom.k8s.model.SbomGenerationStatus;
import org.jboss.sbomer.service.feature.sbom.k8s.reconciler.BuildController;
import org.jboss.sbomer.service.feature.sbom.k8s.resources.Labels;
import org.jboss.sbomer.service.feature.sbom.model.Sbom;
import org.jboss.sbomer.service.feature.sbom.model.SbomGenerationRequest;
import org.jboss.sbomer.service.feature.sbom.service.SbomRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import io.fabric8.knative.internal.pkg.apis.ConditionBuilder;
import io.fabric8.tekton.pipeline.v1beta1.ParamBuilder;
import io.fabric8.tekton.pipeline.v1beta1.ParamValue;
import io.fabric8.tekton.pipeline.v1beta1.TaskRun;
import io.fabric8.tekton.pipeline.v1beta1.TaskRunBuilder;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;

class BuildControllerTest {

    private GenerationRequest generationRequest() {
        return new GenerationRequestBuilder(GenerationRequestType.BUILD).withId("CUSTOMID")
                .withStatus(SbomGenerationStatus.GENERATING)
                .withConfig(Config.fromString("{\"type\": \"pnc-build\"}", PncBuildConfig.class))
                .build();
    }

    private static TaskRun taskRun(String name, String index, String condition) {
        return new TaskRunBuilder().withNewMetadata()
                .withName(name)
                .withLabels(Map.of(Labels.LABEL_PHASE, "generate"))
                .endMetadata()
                .withNewStatus()
                .withConditions(new ConditionBuilder().withStatus(condition).build())
                .endStatus()
                .withNewSpec()
                .withParams(new ParamBuilder().withName("index").withValue(new ParamValue(index)).build())
                .endSpec()
                .build();
    }

    @Test
    void testUpdateStatusIfNotSet() throws Exception {
        BuildController bc = new BuildController();

        @SuppressWarnings("unchecked")
        Context<GenerationRequest> contextMock = Mockito.mock(Context.class);
        GenerationRequest generationRequest = generationRequest();

        generationRequest.setStatus(null);

        assertNull(generationRequest.getStatus());

        UpdateControl<GenerationRequest> control = bc.reconcile(generationRequest, contextMock);

        assertEquals(SbomGenerationStatus.NEW, generationRequest.getStatus());
        assertEquals("NEW", generationRequest.getMetadata().getLabels().get(Labels.LABEL_STATUS));

        assertFalse(control.isNoUpdate());
        assertFalse(control.isUpdateStatus());
        assertTrue(control.isUpdateResource());
    }

    @Test
    void ensureFailedNoTaskRuns() throws Exception {
        SbomGenerationRequest request = new SbomGenerationRequest();

        // Mock syncing with DB
        try (MockedStatic<SbomGenerationRequest> sbomGenerationRequest = Mockito
                .mockStatic(SbomGenerationRequest.class)) {
            sbomGenerationRequest.when(() -> SbomGenerationRequest.sync(any())).thenReturn(request);

            BuildController bc = new BuildController();

            @SuppressWarnings("unchecked")
            Context<GenerationRequest> contextMock = Mockito.mock(Context.class);

            GenerationRequest generationRequest = generationRequest();
            UpdateControl<GenerationRequest> control = bc.reconcile(generationRequest, contextMock);

            assertEquals(SbomGenerationStatus.FAILED, generationRequest.getStatus());
            assertEquals(
                    "Generation failed. Expected one or more running TaskRun related to generation. None found. See logs for more information.",
                    generationRequest.getReason());
            assertEquals(GenerationResult.ERR_SYSTEM, generationRequest.getResult());

            assertFalse(control.isNoUpdate());
            assertFalse(control.isUpdateStatus());
            assertTrue(control.isUpdateResource());
        }
    }

    @Test
    void testSetReasonForMultipleManifestsFinished() throws Exception {
        BuildController bc = new BuildController();

        bc.setNotificationService(mock(NotificationService.class));
        bc.setAtlasHandler(mock(AtlasHandler.class));

        GenerationRequestControllerConfig controllerConfig = Mockito.mock(GenerationRequestControllerConfig.class);
        when(controllerConfig.sbomDir()).thenReturn("/a/dir");

        bc.setControllerConfig(controllerConfig);

        @SuppressWarnings("unchecked")
        Context<GenerationRequest> contextMock = Mockito.mock(Context.class);

        ArgumentCaptor<Sbom> sbomCaptor = ArgumentCaptor.forClass(Sbom.class);

        SbomRepository sbomRepository = Mockito.mock(SbomRepository.class);

        Sbom sbom1 = new Sbom();
        sbom1.setId("AAAID1");

        Sbom sbom2 = new Sbom();
        sbom2.setId("AAAID2");

        when(sbomRepository.saveSbom(sbomCaptor.capture())).thenReturn(sbom1, sbom2);

        bc.setSbomRepository(sbomRepository);

        TaskRun taskRun1 = taskRun("tr1", "0", "True");
        TaskRun taskRun2 = taskRun("tr2", "1", "True");

        SbomGenerationRequest request = new SbomGenerationRequest();
        request.setStatus(SbomGenerationStatus.GENERATING);

        // Mock syncing with DB
        try (MockedStatic<SbomGenerationRequest> sbomGenerationRequest = Mockito
                .mockStatic(SbomGenerationRequest.class)) {
            sbomGenerationRequest.when(() -> SbomGenerationRequest.sync(any())).thenReturn(request);

            try (MockedStatic<SbomUtils> utils = Mockito.mockStatic(SbomUtils.class)) {
                utils.when(() -> SbomUtils.fromPath(any())).thenReturn(new Bom());

                when(contextMock.getSecondaryResources(TaskRun.class)).thenReturn(Set.of(taskRun1, taskRun2));

                GenerationRequest generationRequest = generationRequest();

                generationRequest.setConfig(
                        Config.fromString(TestResources.asString("configs/multi-product.yaml"), PncBuildConfig.class));

                UpdateControl<GenerationRequest> control = bc.reconcile(generationRequest, contextMock);

                assertEquals(SbomGenerationStatus.FINISHED, generationRequest.getStatus());
                assertEquals("FINISHED", generationRequest.getMetadata().getLabels().get(Labels.LABEL_STATUS));
                assertEquals(
                        "Generation finished successfully. Generated SBOMs: AAAID1, AAAID2",
                        generationRequest.getReason());

                assertFalse(control.isNoUpdate());
                assertFalse(control.isUpdateStatus());
                assertTrue(control.isUpdateResource());
            }
        }
    }

    private static Stream<Arguments> provideTaskRuns() {
        return Stream.of(
                Arguments.of(
                        Set.of(taskRun("tr1", "0", "True"), taskRun("tr2", "1", "False")),
                        "Generation request failed. Some tasks failed. Product with index '1' (TaskRun 'tr2') failed: system failure. See logs for more information."),
                Arguments.of(
                        Set.of(taskRun("tr1", "0", "True"), taskRun("tr2", "1", "False"), taskRun("tr3", "2", "False")),
                        "Generation request failed. Some tasks failed. Product with index '1' (TaskRun 'tr2') failed: system failure. Product with index '2' (TaskRun 'tr3') failed: system failure. See logs for more information."));
    }

    @ParameterizedTest
    @MethodSource("provideTaskRuns")
    void testStatusOfFailedGeneration(Set<TaskRun> taskRuns, String reason) throws Exception {
        BuildController bc = new BuildController();

        GenerationRequestControllerConfig controllerConfig = Mockito.mock(GenerationRequestControllerConfig.class);
        when(controllerConfig.sbomDir()).thenReturn("/a/dir");

        bc.setControllerConfig(controllerConfig);

        @SuppressWarnings("unchecked")
        Context<GenerationRequest> contextMock = Mockito.mock(Context.class);

        SbomGenerationRequest request = new SbomGenerationRequest();
        request.setStatus(SbomGenerationStatus.GENERATING);

        // Mock syncing with DB
        try (MockedStatic<SbomGenerationRequest> sbomGenerationRequest = Mockito
                .mockStatic(SbomGenerationRequest.class)) {
            sbomGenerationRequest.when(() -> SbomGenerationRequest.sync(any())).thenReturn(request);

            try (MockedStatic<SbomUtils> utils = Mockito.mockStatic(SbomUtils.class)) {
                utils.when(() -> SbomUtils.fromPath(any())).thenReturn(new Bom());

                when(contextMock.getSecondaryResources(TaskRun.class)).thenReturn(taskRuns);

                GenerationRequest generationRequest = generationRequest();

                generationRequest.setConfig(
                        Config.fromString(TestResources.asString("configs/multi-product.yaml"), PncBuildConfig.class));

                UpdateControl<GenerationRequest> control = bc.reconcile(generationRequest, contextMock);

                assertEquals(SbomGenerationStatus.FAILED, generationRequest.getStatus());
                assertEquals("FAILED", generationRequest.getMetadata().getLabels().get(Labels.LABEL_STATUS));
                assertEquals(reason, generationRequest.getReason());

                assertFalse(control.isNoUpdate());
                assertFalse(control.isUpdateStatus());
                assertTrue(control.isUpdateResource());
            }
        }
    }

}
