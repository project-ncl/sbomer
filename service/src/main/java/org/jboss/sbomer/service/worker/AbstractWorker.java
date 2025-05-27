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
package org.jboss.sbomer.service.worker;

import java.util.Objects;

import org.eclipse.microprofile.context.ManagedExecutor;
import org.jboss.sbomer.service.feature.sbom.model.v1beta2.dto.EventRecord;

import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.TransactionPhase;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractWorker implements Worker {

    ManagedExecutor managedExecutor;

    public AbstractWorker(ManagedExecutor managedExecutor) {
        this.managedExecutor = managedExecutor;
    }

    public void onEvent(@Observes(during = TransactionPhase.AFTER_COMPLETION) EventRecord eventRecord) {
        if (eventRecord == null || eventRecord.metadata() == null
                || !Objects.equals(eventRecord.metadata().get(Worker.KEY_WORKER), getType())) {
            log.debug("Not an event for '{}' worker, skipping", getType());
            return;
        }

        log.info("Handling new {} event with metadata: {}", getType(), eventRecord.metadata());

        managedExecutor.runAsync(() -> {
            log.info("Worker is handling the event, currently NOOP");
        });
    }
}
