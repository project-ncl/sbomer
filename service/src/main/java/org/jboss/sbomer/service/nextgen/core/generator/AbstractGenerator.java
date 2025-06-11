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

import org.eclipse.microprofile.context.ManagedExecutor;
import org.jboss.sbomer.service.nextgen.core.dto.model.EventRecord;
import org.jboss.sbomer.service.nextgen.core.dto.model.GenerationRecord;
import org.jboss.sbomer.service.nextgen.core.enums.GenerationResult;
import org.jboss.sbomer.service.nextgen.core.enums.GenerationStatus;
import org.jboss.sbomer.service.nextgen.core.events.GenerationScheduledEvent;
import org.jboss.sbomer.service.nextgen.core.payloads.generation.GenerationStatusUpdatePayload;
import org.jboss.sbomer.service.nextgen.core.rest.SBOMerClient;

import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.TransactionPhase;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractGenerator implements Generator {

    ManagedExecutor managedExecutor;
    protected SBOMerClient sbomerClient;

    public AbstractGenerator(SBOMerClient sbomerClient, ManagedExecutor managedExecutor) {
        this.sbomerClient = sbomerClient;
        this.managedExecutor = managedExecutor;
    }

    public abstract void handle(EventRecord event, GenerationRecord generation);

    @Override
    public void onEvent(@Observes(during = TransactionPhase.AFTER_SUCCESS) GenerationScheduledEvent event) {
        if (!event.generation().isSupported(getTypes())) {
            // This is not an event handled by this generator
            return;
        }

        log.info("Handling new generation with data: {}", event.generation());

        managedExecutor.runAsync(() -> {
            try {
                updateStatus(event.generation().id(), GenerationStatus.GENERATING, null, "Generation started");

                handle(event.event(), event.generation());
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

    // TODO: This should be retried in case of failures
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
