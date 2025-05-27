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
package org.jboss.sbomer.service.v1beta2.resolver;

import java.util.Objects;

import org.eclipse.microprofile.context.ManagedExecutor;
import org.jboss.sbomer.service.feature.sbom.model.v1beta2.Event;
import org.jboss.sbomer.service.feature.sbom.model.v1beta2.EventStatusHistory;
import org.jboss.sbomer.service.feature.sbom.model.v1beta2.dto.EventRecord;
import org.jboss.sbomer.service.feature.sbom.model.v1beta2.enums.EventStatus;

import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.TransactionPhase;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@NoArgsConstructor
public abstract class AbstractResolver implements Resolver {

    ManagedExecutor managedExecutor;


    public AbstractResolver(ManagedExecutor managedExecutor) {
        this.managedExecutor = managedExecutor;
    }

    /**
     * This will be replaced by receiving an event from Kafka.
     *
     * @param eventRecord
     */
    public void onEvent(@Observes(during = TransactionPhase.AFTER_COMPLETION) EventRecord eventRecord) {
        if (eventRecord == null || eventRecord.metadata() == null
                || !Objects.equals(eventRecord.metadata().get(Resolver.KEY_RESOLVER), getType())) {
            log.debug("Not an event for '{}' resolver, skipping", getType());
            return;
        }

        log.info("Handling new {} event with metadata: {}", getType(), eventRecord.metadata());

        String identifier = eventRecord.metadata().get(Resolver.KEY_IDENTIFIER);

        if (identifier == null || identifier.trim().isEmpty()) {
            log.warn("Identifier missing, event won't be processed");
            return;
        }

        log.info("Processing event for identifier {}...", identifier);

        managedExecutor.runAsync(() -> {
            try {
                updateEventStatus(eventRecord.id(), EventStatus.RESOLVING);
                resolve(eventRecord.id(), identifier);
                updateEventStatus(eventRecord.id(), EventStatus.RESOLVED);
            } catch (Exception e) {
                updateEventStatus(eventRecord.id(), EventStatus.ERROR);
            }
        });
    }

    @Transactional(value = TxType.REQUIRES_NEW)
    protected EventStatusHistory updateEventStatus(String eventId, EventStatus status) {
        Event event = Event.findById(eventId);

        if (event == null) {
            log.warn("Event with id '{}' could not be found, cannot update status", eventId);
            return null;
        }

        event.setStatus(status);

        return new EventStatusHistory(event, status.name(), "Updated by resolver").save();
    }
}
