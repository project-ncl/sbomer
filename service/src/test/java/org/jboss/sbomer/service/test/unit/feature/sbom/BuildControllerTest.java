package org.jboss.sbomer.service.test.unit.feature.sbom;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Set;

import org.cyclonedx.model.Bom;
import org.jboss.sbomer.core.features.sbom.config.Config;
import org.jboss.sbomer.core.features.sbom.config.PncBuildConfig;
import org.jboss.sbomer.core.features.sbom.enums.GenerationRequestType;
import org.jboss.sbomer.core.features.sbom.enums.GenerationResult;
import org.jboss.sbomer.core.features.sbom.utils.SbomUtils;
import org.jboss.sbomer.core.test.TestResources;
import org.jboss.sbomer.service.feature.sbom.config.GenerationRequestControllerConfig;
import org.jboss.sbomer.service.feature.sbom.k8s.model.GenerationRequest;
import org.jboss.sbomer.service.feature.sbom.k8s.model.GenerationRequestBuilder;
import org.jboss.sbomer.service.feature.sbom.k8s.model.SbomGenerationStatus;
import org.jboss.sbomer.service.feature.sbom.k8s.reconciler.BuildController;
import org.jboss.sbomer.service.feature.sbom.k8s.resources.Labels;
import org.jboss.sbomer.service.feature.sbom.model.Sbom;
import org.jboss.sbomer.service.feature.sbom.model.SbomGenerationRequest;
import org.jboss.sbomer.service.feature.sbom.service.SbomRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import io.fabric8.knative.internal.pkg.apis.ConditionBuilder;
import io.fabric8.tekton.pipeline.v1beta1.TaskRun;
import io.fabric8.tekton.pipeline.v1beta1.TaskRunBuilder;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;

public class BuildControllerTest {

    private GenerationRequest generationRequest() {
        return new GenerationRequestBuilder(GenerationRequestType.BUILD).withId("CUSTOMID")
                .withStatus(SbomGenerationStatus.GENERATING)
                .withConfig(Config.fromString("{}", PncBuildConfig.class))
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

        TaskRun taskRun1 = new TaskRunBuilder().withNewMetadata()
                .withName("tr1")
                .withLabels(Map.of(Labels.LABEL_PHASE, "generate"))
                .endMetadata()
                .withNewStatus()
                .withConditions(new ConditionBuilder().withStatus("True").build())
                .endStatus()
                .build();
        TaskRun taskRun2 = new TaskRunBuilder().withNewMetadata()
                .withName("tr2")
                .withLabels(Map.of(Labels.LABEL_PHASE, "generate"))
                .endMetadata()
                .withNewStatus()
                .withConditions(new ConditionBuilder().withStatus("True").build())
                .endStatus()
                .build();

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
                assertEquals("Generation finished successfully. Generated SBOMs: AAAID1, AAAID2", generationRequest.getReason());

                assertFalse(control.isNoUpdate());
                assertFalse(control.isUpdateStatus());
                assertTrue(control.isUpdateResource());
            }
        }
    }
}
