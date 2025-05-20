package org.jboss.sbomer.service.resolver;

import java.util.Objects;

import org.eclipse.microprofile.context.ManagedExecutor;
import org.jboss.sbomer.service.feature.sbom.model.v1beta2.Event;
import org.jboss.sbomer.service.feature.sbom.model.v1beta2.dto.EventRecord;
import org.jboss.sbomer.service.feature.sbom.model.v1beta2.enums.EventStatus;

import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.TransactionPhase;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractResolver implements Resolver {

    ManagedExecutor managedExecutor;

    public AbstractResolver() {

    }

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
    protected Event updateEventStatus(String eventId, EventStatus status) {
        Event event = Event.findById(eventId);

        if (event == null) {
            log.warn("Event with id '{}' could not be found, cannot update status", eventId);
            return null;
        }

        event.setStatus(status);

        return event;
    }
}
