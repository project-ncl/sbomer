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
package org.jboss.sbomer.service.feature.sbom.features.generator.image.syft.controller;

import static org.jboss.sbomer.service.feature.sbom.features.generator.AbstractController.EVENT_SOURCE_NAME;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.cyclonedx.model.Bom;
import org.jboss.sbomer.core.features.sbom.enums.GenerationRequestType;
import org.jboss.sbomer.core.features.sbom.enums.GenerationResult;
import org.jboss.sbomer.core.features.sbom.utils.SbomUtils;
import org.jboss.sbomer.service.feature.sbom.features.generator.AbstractController;
import org.jboss.sbomer.service.feature.sbom.k8s.model.GenerationRequest;
import org.jboss.sbomer.service.feature.sbom.k8s.model.SbomGenerationPhase;
import org.jboss.sbomer.service.feature.sbom.k8s.model.SbomGenerationStatus;
import org.jboss.sbomer.service.feature.sbom.k8s.resources.Labels;
import org.jboss.sbomer.service.feature.sbom.model.Sbom;

import io.fabric8.tekton.pipeline.v1beta1.TaskRun;
import io.javaoperatorsdk.operator.api.reconciler.Constants;
import io.javaoperatorsdk.operator.api.reconciler.ControllerConfiguration;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;
import io.javaoperatorsdk.operator.api.reconciler.dependent.Dependent;
import jakarta.validation.ValidationException;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>
 * Reconciler working on the {@link GenerationRequest} entity and the {@link GenerationRequestType#CONTAINERIMAGE} type.
 * </p>
 *
 * <p>
 * This reconciler acts only on resources marked with following labels (all of them must exist on the resource):
 *
 * <ul>
 * <li>{@code app.kubernetes.io/part-of=sbomer}</li>
 * <li>{@code app.kubernetes.io/component=sbom}</li>
 * <li>{@code app.kubernetes.io/managed-by=sbom}</li>
 * <li>{@code sbomer.jboss.org/generation-request}</li>
 * <li>{@code sbomer.jboss.org/generation-request-type}</li>
 * </ul>
 * </p>
 */
@ControllerConfiguration(
        labelSelector = "app.kubernetes.io/part-of=sbomer,app.kubernetes.io/component=sbom,app.kubernetes.io/managed-by=sbom,sbomer.jboss.org/type=generation-request,sbomer.jboss.org/generation-request-type=containerimage",
        namespaces = { Constants.WATCH_CURRENT_NAMESPACE },

        dependents = { @Dependent(
                type = TaskRunSyftImageGenerateDependentResource.class,
                useEventSourceWithName = EVENT_SOURCE_NAME)

        })
@Slf4j
public class SyftImageController extends AbstractController {
    @Override
    protected UpdateControl<GenerationRequest> updateRequest(
            GenerationRequest generationRequest,
            SbomGenerationStatus status,
            GenerationResult result,
            String reason) {

        if (generationRequest.getStatus() != null) {
            String label = switch (generationRequest.getStatus()) {
                case GENERATING -> SbomGenerationPhase.GENERATE.name().toLowerCase();
                default -> null;
            };

            if (label != null) {
                generationRequest.getMetadata().getLabels().put(Labels.LABEL_PHASE, label);
            }
        }

        generationRequest.setStatus(status);
        generationRequest.setResult(result);
        generationRequest.setReason(reason);
        return UpdateControl.updateResource(generationRequest);
    }

    /**
     * <p>
     * Handles updates to {@link GenerationRequest} being in progress.
     * </p>
     *
     * @param generationRequest
     * @param secondaryResources
     * @return
     */
    @Override
    protected UpdateControl<GenerationRequest> reconcileGenerating(
            GenerationRequest generationRequest,
            Set<TaskRun> secondaryResources) {

        log.debug("Reconcile GENERATING for '{}'...", generationRequest.getName());

        TaskRun generateTaskRun = findTaskRun(secondaryResources, SbomGenerationPhase.GENERATE);

        if (generateTaskRun == null) {
            log.error("There is no generation TaskRun related to GenerationRequest '{}'", generationRequest.getName());

            return updateRequest(
                    generationRequest,
                    SbomGenerationStatus.FAILED,
                    GenerationResult.ERR_SYSTEM,
                    "Generation failed. Unable to find related TaskRun. See logs for more information.");
        }

        // In case the TaskRun hasn't finished yet, wait for next update.
        if (!isFinished(generateTaskRun)) {
            return UpdateControl.noUpdate();
        }

        // In case the Task Run is not successfull, fail thge generation
        if (!isSuccessful(generateTaskRun)) {
            log.error("Generation failed, the TaskRun returnd failure");

            return updateRequest(
                    generationRequest,
                    SbomGenerationStatus.FAILED,
                    GenerationResult.ERR_SYSTEM,
                    "Generation failed. TaskRun responsible for generation failed. See logs for more information.");
        }

        // Construct the path to the working directory of the generator
        Path generationDir = Path.of(controllerConfig.sbomDir(), generationRequest.getMetadata().getName());

        log.debug("Reading manifests from '{}'...", generationDir.toAbsolutePath());

        List<Sbom> sboms = new ArrayList<>();

        PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:**/bom.json");

        try {
            Files.walkFileTree(generationDir, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {

                    if (matcher.matches(path)) {
                        log.info("Found manifest at path '{}'", path.toAbsolutePath());

                        // Read the generated SBOM JSON file
                        Bom bom = SbomUtils.fromPath(path);
                        // Store the manifest in database
                        sboms.add(storeSbom(generationRequest, bom));

                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            log.error("Unexpected error ocurred", e);

            return updateRequest(
                    generationRequest,
                    SbomGenerationStatus.FAILED,
                    GenerationResult.ERR_GENERATION,
                    "Generation failed. One or more generated SBOMs failed due IO exception. See logs for more information.");
        } catch (ValidationException e) {
            // There was an error when validating the entity, most probably the SBOM is not valid
            log.error("Unable to validate generated SBOM", e);

            return updateRequest(
                    generationRequest,
                    SbomGenerationStatus.FAILED,
                    GenerationResult.ERR_GENERATION,
                    "Generation failed. One or more generated SBOMs failed validation. See logs for more information.");
        }

        return updateRequest(
                generationRequest,
                SbomGenerationStatus.FINISHED,
                GenerationResult.SUCCESS,
                String.format(
                        "Generation finished successfully. Generated SBOMs: %s",
                        sboms.stream().map(sbom -> sbom.getId()).collect(Collectors.joining(", "))));
    }
}
