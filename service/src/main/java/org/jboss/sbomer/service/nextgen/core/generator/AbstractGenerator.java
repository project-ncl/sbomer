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
import org.jboss.sbomer.service.nextgen.core.enums.GenerationStatus;
import org.jboss.sbomer.service.nextgen.core.events.GenerationScheduledEvent;
import org.jboss.sbomer.service.nextgen.service.model.Generation;
import org.slf4j.helpers.MessageFormatter;

import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.TransactionPhase;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@NoArgsConstructor
public abstract class AbstractGenerator implements Generator {

    ManagedExecutor managedExecutor;

    public AbstractGenerator(ManagedExecutor managedExecutor) {
        this.managedExecutor = managedExecutor;
    }

    public void onEvent(@Observes(during = TransactionPhase.AFTER_SUCCESS) GenerationScheduledEvent event) {
        if (!event.generation().isOfRequestType(getType())) {
            // This is not an event handled by this generator
            return;
        }

        log.info("Handling new {} event with data: {}", getType(), event.generation());

        managedExecutor.runAsync(() -> {
            try {
                updateGenerationStatus(event.generation().id(), GenerationStatus.GENERATING, "Generation started");
                handle(event.event(), event.generation());
            } catch (Exception e) {
                log.error("Unable to generate", e);

                updateGenerationStatus(
                        event.generation().id(),
                        GenerationStatus.FAILED,
                        "Generation failed, reason: {}",
                        e.getMessage());

            }
        });
    }

    @Transactional(value = TxType.REQUIRES_NEW)
    protected void updateGenerationStatus(
            String generationId,
            GenerationStatus status,
            String reason,
            Object... params) {
        Generation generation = Generation.findById(generationId);

        if (generation == null) {
            log.warn("Generation with id '{}' could not be found, cannot update status", generationId);
            return;
        }

        generation.setStatus(status);
        generation.setReason(MessageFormatter.arrayFormat(reason, params).getMessage());
        generation.save();
    }

}
