package org.jboss.sbomer.service.test.unit.feature.sbom.syftimage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

import org.cyclonedx.model.Bom;
import org.jboss.sbomer.core.features.sbom.config.Config;
import org.jboss.sbomer.core.features.sbom.config.SyftImageConfig;
import org.jboss.sbomer.core.features.sbom.enums.GenerationRequestType;
import org.jboss.sbomer.core.features.sbom.enums.GenerationResult;
import org.jboss.sbomer.core.features.sbom.utils.FileUtils;
import org.jboss.sbomer.core.test.TestResources;
import org.jboss.sbomer.service.feature.sbom.atlas.AtlasHandler;
import org.jboss.sbomer.service.feature.sbom.config.GenerationRequestControllerConfig;
import org.jboss.sbomer.service.feature.sbom.features.umb.producer.NotificationService;
import org.jboss.sbomer.service.feature.sbom.k8s.model.GenerationRequest;
import org.jboss.sbomer.service.feature.sbom.k8s.model.GenerationRequestBuilder;
import org.jboss.sbomer.service.feature.sbom.k8s.model.SbomGenerationStatus;
import org.jboss.sbomer.service.feature.sbom.k8s.resources.Labels;
import org.jboss.sbomer.service.feature.sbom.model.Sbom;
import org.jboss.sbomer.service.feature.sbom.model.SbomGenerationRequest;
import org.jboss.sbomer.service.feature.sbom.service.SbomRepository;
import org.jboss.sbomer.service.generator.image.controller.SyftImageController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import io.fabric8.knative.pkg.apis.ConditionBuilder;
import io.fabric8.tekton.v1beta1.ParamBuilder;
import io.fabric8.tekton.v1beta1.ParamValue;
import io.fabric8.tekton.v1beta1.TaskRun;
import io.fabric8.tekton.v1beta1.TaskRunBuilder;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;
import jakarta.enterprise.inject.Vetoed;

class SyftImageControllerTest {
    static class MockSbomRepository extends SbomRepository {
        @Override
        public List<Sbom> saveSboms(List<Sbom> sboms) {
            return sboms;
        }
    }

    @Vetoed
    static class SyftImageControllerAlt extends SyftImageController {
        @Override
        public UpdateControl<GenerationRequest> reconcileGenerating(
                GenerationRequest generationRequest,
                Set<TaskRun> secondaryResources) {
            return super.reconcileGenerating(generationRequest, secondaryResources);
        }

        @Override
        public List<Bom> readManifests(List<Path> manifestPaths) {
            return super.readManifests(manifestPaths);
        }
    }

    private static final GenerationRequest GENERATION_REQUEST = new GenerationRequestBuilder(
            GenerationRequestType.BUILD).withId("CUSTOMID")
            .withStatus(SbomGenerationStatus.GENERATING)
            .withConfig(Config.fromString("{\"type\": \"syft-image\"}", SyftImageConfig.class))
            .build();

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

    SyftImageControllerAlt controller;

    @BeforeEach
    void beforeEach() {
        controller = new SyftImageControllerAlt();
    }

    @Test
    void shouldNotFindAnything(@TempDir Path tmpDir) throws Exception {
        Path aFile = tmpDir.resolve("init.log");
        Files.write(aFile, "Some content".getBytes());

        assertEquals(0, FileUtils.findManifests(tmpDir).size());
    }

    @Test
    void findSomeManifests(@TempDir Path tmpDir) throws Exception {
        Path oneDir = tmpDir.resolve("one");
        Files.createDirectory(oneDir);

        Path twoDir = tmpDir.resolve("two");
        Files.createDirectory(twoDir);

        Path threeDir = twoDir.resolve("three");
        Files.createDirectory(threeDir);

        Path twoManifest = twoDir.resolve("bom.json");
        Path threeManifest = threeDir.resolve("bom.json");

        Files.write(twoManifest, "{}".getBytes());
        Files.write(threeManifest, "{}".getBytes());

        List<Path> manifests = FileUtils.findManifests(tmpDir);

        assertEquals(2, manifests.size());
        assertTrue(manifests.get(0).endsWith("two/bom.json"));
        assertTrue(manifests.get(1).endsWith("two/three/bom.json"));
    }

    @Test
    void shouldReadManifests(@TempDir Path tmpDir) throws Exception {
        Path manifest1 = tmpDir.resolve("1.json");
        Files.write(manifest1, TestResources.asString("sboms/complete_sbom.json").getBytes());

        Path manifest2 = tmpDir.resolve("2.json");
        Files.write(manifest2, TestResources.asString("sboms/complete_operation_sbom.json").getBytes());

        List<Bom> boms = controller.readManifests(List.of(manifest1, manifest2));

        assertEquals(2, boms.size());
        assertEquals(
                "pkg:maven/org.eclipse.microprofile.graphql/microprofile-graphql-parent@1.1.0.redhat-00008?type=pom",
                boms.get(0).getMetadata().getComponent().getPurl());
        assertEquals(
                "pkg:generic/my-broker-7.11.5.CR3-bin.zip@7.11.5.CR3?operation=A5RPHL7Y3AIAA",
                boms.get(1).getMetadata().getComponent().getPurl());
    }

    private void withController(Path tmpDir, BiConsumer<Path, SyftImageControllerAlt> consumer) throws IOException {

        Path requestDir = tmpDir.resolve("sbom-request-" + GENERATION_REQUEST.getId().toLowerCase());
        Files.createDirectory(requestDir);

        GenerationRequestControllerConfig controllerConfig = Mockito.mock(GenerationRequestControllerConfig.class);
        when(controllerConfig.sbomDir()).thenReturn(tmpDir.toString());

        SyftImageControllerAlt ctrl = new SyftImageControllerAlt();
        ctrl.setNotificationService(mock(NotificationService.class));
        ctrl.setAtlasHandler(mock(AtlasHandler.class));
        ctrl.setControllerConfig(controllerConfig);
        ctrl.setSbomRepository(new MockSbomRepository());

        try (MockedStatic<SbomGenerationRequest> sbomGenerationRequest = Mockito
                .mockStatic(SbomGenerationRequest.class)) {
            sbomGenerationRequest.when(() -> SbomGenerationRequest.sync(any())).thenReturn(new SbomGenerationRequest());
            consumer.accept(requestDir, ctrl);
        }
    }

    @Test
    void shouldReconcileNoManifestsFound(@TempDir Path tmpDir) throws IOException {
        withController(tmpDir, (requestDir, ctrl) -> {
            UpdateControl<GenerationRequest> updateControl = ctrl.reconcileGenerating(
                    GENERATION_REQUEST,
                    Set.of(SyftImageControllerTest.taskRun("tr1", "0", "True")));

            assertFalse(updateControl.isNoUpdate());
            assertEquals(SbomGenerationStatus.FAILED, updateControl.getResource().get().getStatus());
            assertEquals(
                    "Generation succeed, but no manifests could be found. At least one was expected. See logs for more information.",
                    updateControl.getResource().get().getReason());
            assertEquals(GenerationResult.ERR_SYSTEM, updateControl.getResource().get().getResult());
        });
    }

    @Test
    void shouldReconcileOneManifest(@TempDir Path tmpDir) throws IOException {
        withController(tmpDir, (requestDir, ctrl) -> {
            Path manifest1 = requestDir.resolve("bom.json");

            try {
                Files.write(manifest1, TestResources.asString("sboms/complete_sbom.json").getBytes());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            UpdateControl<GenerationRequest> updateControl = ctrl.reconcileGenerating(
                    GENERATION_REQUEST,
                    Set.of(SyftImageControllerTest.taskRun("tr1", "0", "True")));

            assertFalse(updateControl.isNoUpdate());
            assertEquals(SbomGenerationStatus.FINISHED, updateControl.getResource().get().getStatus());
            assertTrue(
                    updateControl.getResource()
                            .get()
                            .getReason()
                            .startsWith("Generation finished successfully. Generated SBOMs: "));
            assertEquals(GenerationResult.SUCCESS, updateControl.getResource().get().getResult());
        });
    }
}
