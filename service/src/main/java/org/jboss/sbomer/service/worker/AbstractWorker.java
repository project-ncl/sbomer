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
