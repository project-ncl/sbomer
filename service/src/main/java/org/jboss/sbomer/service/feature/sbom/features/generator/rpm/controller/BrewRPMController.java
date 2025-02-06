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
package org.jboss.sbomer.service.feature.sbom.features.generator.rpm.controller;

import static org.jboss.sbomer.service.feature.sbom.features.generator.AbstractController.EVENT_SOURCE_NAME;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.cyclonedx.model.Bom;
import org.jboss.sbomer.core.errors.ApplicationException;
import org.jboss.sbomer.core.features.sbom.enums.GenerationRequestType;
import org.jboss.sbomer.core.features.sbom.enums.GenerationResult;
import org.jboss.sbomer.core.features.sbom.utils.FileUtils;
import org.jboss.sbomer.service.feature.sbom.features.generator.AbstractController;
import org.jboss.sbomer.service.feature.sbom.k8s.model.GenerationRequest;
import org.jboss.sbomer.service.feature.sbom.k8s.model.SbomGenerationPhase;
import org.jboss.sbomer.service.feature.sbom.k8s.model.SbomGenerationStatus;
import org.jboss.sbomer.service.feature.sbom.k8s.resources.Labels;
import org.jboss.sbomer.service.feature.sbom.model.Sbom;
import org.slf4j.helpers.MessageFormatter;

import io.fabric8.tekton.pipeline.v1beta1.TaskRun;
import io.javaoperatorsdk.operator.api.reconciler.Constants;
import io.javaoperatorsdk.operator.api.reconciler.ControllerConfiguration;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;
import io.javaoperatorsdk.operator.api.reconciler.dependent.Dependent;
import jakarta.validation.ValidationException;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>
 * Reconciler working on the {@link GenerationRequest} entity and the {@link GenerationRequestType#BREW_RPM} type.
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
        labelSelector = "app.kubernetes.io/part-of=sbomer,app.kubernetes.io/component=sbom,app.kubernetes.io/managed-by=sbom,sbomer.jboss.org/type=generation-request,sbomer.jboss.org/generation-request-type=brew-rpm",
        namespaces = { Constants.WATCH_CURRENT_NAMESPACE },
        dependents = { @Dependent(
                type = TaskRunBrewRPMGenerateDependentResource.class,
                useEventSourceWithName = EVENT_SOURCE_NAME) })
@Slf4j
public class BrewRPMController extends AbstractController {

    @Override
    protected UpdateControl<GenerationRequest> updateRequest(
            GenerationRequest generationRequest,
            SbomGenerationStatus status,
            GenerationResult result,
            String reason,
            Object... params) {

        if (generationRequest.getStatus() != null) {
            String label = generationRequest.getStatus() == SbomGenerationStatus.GENERATING
                    ? SbomGenerationPhase.GENERATE.name().toLowerCase()
                    : null;

            if (label != null) {
                generationRequest.getMetadata().getLabels().put(Labels.LABEL_PHASE, label);
            }
        }

        generationRequest.setStatus(status);
        generationRequest.setResult(result);
        generationRequest.setReason(MessageFormatter.arrayFormat(reason, params).getMessage());
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
            log.error("Generation failed, the TaskRun returned failure");

            return updateRequest(
                    generationRequest,
                    SbomGenerationStatus.FAILED,
                    GenerationResult.ERR_SYSTEM,
                    "Generation failed. TaskRun responsible for generation failed. See logs for more information.");
        }

        // Construct the path to the working directory of the generator
        Path generationDir = Path.of(controllerConfig.sbomDir(), generationRequest.getMetadata().getName());

        log.debug("Reading manifests from '{}'...", generationDir.toAbsolutePath());

        List<Path> manifestPaths;

        try {
            manifestPaths = FileUtils.findManifests(generationDir);
        } catch (IOException e) {
            log.error("Unexpected IO exception ocurred while trying to find generated manifests", e);

            return updateRequest(
                    generationRequest,
                    SbomGenerationStatus.FAILED,
                    GenerationResult.ERR_SYSTEM,
                    "Generation succeded, but reading generated SBOMs failed due IO exception. See logs for more information.");
        }

        if (manifestPaths.isEmpty()) {
            log.error("No manifests found, this is unexpected");

            return updateRequest(
                    generationRequest,
                    SbomGenerationStatus.FAILED,
                    GenerationResult.ERR_SYSTEM,
                    "Generation succeed, but no manifests could be found. At least one was expected. See logs for more information.");
        }

        List<Bom> boms;

        try {
            boms = readManifests(manifestPaths);
        } catch (Exception e) {
            log.error("Unable to read one or more manifests", e);

            return updateRequest(
                    generationRequest,
                    SbomGenerationStatus.FAILED,
                    GenerationResult.ERR_SYSTEM,
                    "Generation succeded, but reading generated manifests failed was not successful. See logs for more information.");
        }

        List<Sbom> sboms;

        try {
            sboms = storeBoms(generationRequest, boms);
        } catch (ValidationException e) {
            // There was an error when validating the entity, most probably the SBOM is not valid
            log.error("Unable to validate generated SBOMs: {}", e.getMessage(), e);

            return updateRequest(
                    generationRequest,
                    SbomGenerationStatus.FAILED,
                    GenerationResult.ERR_GENERATION,
                    "Generation failed. One or more generated SBOMs failed validation: {}. See logs for more information.",
                    e.getMessage());
        }

        try {
            performPost(sboms, generationRequest);
        } catch (ApplicationException e) {
            return updateRequest(
                    generationRequest,
                    SbomGenerationStatus.FAILED,
                    GenerationResult.ERR_POST,
                    e.getMessage());
        }

        return updateRequest(
                generationRequest,
                SbomGenerationStatus.FINISHED,
                GenerationResult.SUCCESS,
                String.format(
                        "Generation finished successfully. Generated SBOMs: %s",
                        sboms.stream().map(Sbom::getId).collect(Collectors.joining(", "))));
    }

}
