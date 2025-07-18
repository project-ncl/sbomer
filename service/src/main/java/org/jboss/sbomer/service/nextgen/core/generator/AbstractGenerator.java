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
package org.jboss.sbomer.service.nextgen.core.generator;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.microprofile.context.ManagedExecutor;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.jboss.sbomer.core.features.sbom.utils.MDCUtils;
import org.jboss.sbomer.service.nextgen.core.dto.api.GenerationRequest;
import org.jboss.sbomer.service.nextgen.core.dto.model.GenerationRecord;
import org.jboss.sbomer.service.nextgen.core.dto.model.ManifestRecord;
import org.jboss.sbomer.service.nextgen.core.enums.GenerationResult;
import org.jboss.sbomer.service.nextgen.core.enums.GenerationStatus;
import org.jboss.sbomer.service.nextgen.core.events.GenerationStatusChangeEvent;
import org.jboss.sbomer.service.nextgen.core.payloads.generation.GenerationStatusUpdatePayload;
import org.jboss.sbomer.service.nextgen.core.rest.SBOMerClient;
import org.jboss.sbomer.service.nextgen.core.utils.JacksonUtils;
import org.jboss.sbomer.service.nextgen.service.model.Manifest;

import com.fasterxml.jackson.databind.JsonNode;

import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.TransactionPhase;
import jakarta.ws.rs.ServerErrorException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractGenerator implements Generator {

    ManagedExecutor managedExecutor;
    protected SBOMerClient sbomerClient;

    public AbstractGenerator(SBOMerClient sbomerClient, ManagedExecutor managedExecutor) {
        this.sbomerClient = sbomerClient;
        this.managedExecutor = managedExecutor;
    }

    private boolean isSupportedType(GenerationRequest generationRequest) {
        for (String type : getSupportedTypes()) {

            if (generationRequest.target().type().equals(type)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Method that handles notification for generations. It will receive many events. We need to react only to the ones
     * that we can handle by filtering supported types.
     *
     * TODO: when properly switching to Kafka we need to ensure we filter messages we are able to handle
     *
     * @param event
     */
    public void onEvent(@Observes(during = TransactionPhase.AFTER_SUCCESS) GenerationStatusChangeEvent event) {
        log.debug(
                "Received generation status change event for generation '{}' and status '{}'",
                event.generation().id(),
                event.generation().status());

        if (event.generation().status() != GenerationStatus.SCHEDULED) {
            log.info(
                    "Generation status is '{}', but expected status is '{}', skipping",
                    event.generation().status(),
                    GenerationStatus.SCHEDULED);
            return;
        }

        if (event.generation().request() == null) {
            log.warn(
                    "Generation status change event for generation '{}' does not have a request, skipping",
                    event.generation().id());

            return;
        }

        GenerationRequest generationRequest = JacksonUtils.parse(GenerationRequest.class, event.generation().request());

        if (!isSupportedType(generationRequest)) {
            log.info(
                    "Event type: {} is not supported by the {} generator",
                    generationRequest.target().type(),
                    getGeneratorName());
            return;
        }

        if (generationRequest.generator() == null) {
            log.warn("Event does not have generator specified, skipping");
            return;
        }

        if (!getGeneratorName().equals(generationRequest.generator().name())) {
            log.debug(
                    "Requested generator name: '{}' is different from requested: '{}', skipping",
                    getGeneratorName(),
                    generationRequest.generator().name());
            return;
        }

        if (!getGeneratorVersion().equals(generationRequest.generator().version())) {
            log.debug(
                    "Requested generator version: '{}' is different from this generator's version: '{}', skipping",
                    getGeneratorVersion(),
                    generationRequest.generator().version());
            return;
        }

        log.info("Handling new generation with data: {}", event.generation());

        managedExecutor.runAsync(() -> {
            try {
                updateStatus(
                        event.generation().id(),
                        GenerationStatus.GENERATING,
                        null,
                        "Generation in progress, handled by {}",
                        getGeneratorName());

                generate(event.generation());
            } catch (Exception e) {
                log.error("Unable to generate", e);

                updateStatus(
                        event.generation().id(),
                        GenerationStatus.FAILED,
                        null, // TODO
                        "Generation failed, reason: {}",
                        e.getMessage());
            }
        });
    }

    /**
     * <p>
     * Stores generated manifests in the database which results in creation of new Manifest entities.
     * </p>
     *
     * @param generationREcord the generation request
     * @param boms the BOMs to store
     * @return the list of stored {@link Manifest}s
     */
    public List<ManifestRecord> storeBoms(GenerationRecord generationRecord, List<JsonNode> boms) {
        // TODO @avibelli
        MDCUtils.removeOtelContext();
        MDCUtils.addIdentifierContext(generationRecord.id());
        // MDCUtils.addOtelContext(generation.getMDCOtel());

        // TODO @avibelli
        // Maybe we should add it later, when we will be transitioning this into a release manifest?
        // Syft controller should be generic and not know anything about RH internals.

        // Verify if the request event for this generation is associated with an Errata advisory
        // RequestEvent event = sbomGenerationRequest.getRequest();
        // if (event != null && event.getRequestConfig() != null
        // && event.getRequestConfig() instanceof ErrataAdvisoryRequestConfig config) {

        // boms.forEach(bom -> {
        // // Add the AdvisoryId property
        // addPropertyIfMissing(
        // bom.getMetadata(),
        // Constants.CONTAINER_PROPERTY_ADVISORY_ID,
        // config.getAdvisoryId());
        // });
        // }

        log.info("There are {} manifests to be stored for the '{}' generation...", boms.size(), generationRecord.id());

        List<ManifestRecord> manifests = new ArrayList<>();

        boms.forEach(bom -> {
            log.info("Storing manifests for the Generation '{}'", generationRecord.id());
            manifests.add(sbomerClient.uploadManifest(generationRecord.id(), bom));
        });

        return manifests;
    }

    @Retry(maxRetries = 10, delay = 10, delayUnit = ChronoUnit.SECONDS, retryOn = ServerErrorException.class)
    protected void updateStatus(
            String generationId,
            GenerationStatus status,
            GenerationResult result,
            String reason,
            Object... params) {

        sbomerClient
                .updateGenerationStatus(generationId, GenerationStatusUpdatePayload.of(status, result, reason, params));

    }
}
