package org.jboss.sbomer.service.advisory;

import java.util.List;
import java.util.Objects;

import org.eclipse.microprofile.context.ManagedExecutor;
import org.jboss.sbomer.core.features.sbom.enums.GenerationRequestType;
import org.jboss.sbomer.service.feature.sbom.model.RandomStringIdGenerator;
import org.jboss.sbomer.service.feature.sbom.model.v1beta2.Event;
import org.jboss.sbomer.service.feature.sbom.model.v1beta2.Generation;
import org.jboss.sbomer.service.feature.sbom.model.v1beta2.dto.EventRecord;
import org.jboss.sbomer.service.feature.sbom.model.v1beta2.enums.GenerationStatus;
import org.jboss.sbomer.service.rest.api.v1beta2.EventsV1Beta2;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.TransactionPhase;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;
import lombok.extern.slf4j.Slf4j;

@ApplicationScoped
@Slf4j
public class RedHatAdvisoryEventListener {

    public static final String RESOLVER_TYPE = "rh-advisory";

    @Inject
    ManagedExecutor managedExecutor;

    /**
     * This will be replaced by receiving an event from Kafka.
     * 
     * @param eventRecord
     */
    public void onReleaseAdvisoryEvent(@Observes(during = TransactionPhase.AFTER_COMPLETION) EventRecord eventRecord) {
        if (eventRecord == null || eventRecord.metadata() == null
                || !Objects.equals(eventRecord.metadata().get(EventsV1Beta2.KEY_RESOLVER), RESOLVER_TYPE)) {
            log.debug("Not a Red Hat advisory event, skipping");
            return;
        }

        log.info("Handling new Red Hat advisory event with metadata: {}", eventRecord.metadata());

        String identifier = eventRecord.metadata().get(EventsV1Beta2.KEY_IDENTIFIER);

        if (identifier == null || identifier.trim().isEmpty()) {
            log.warn("Red Hat advisory identifier missing, event won't be processed");
            return;
        }

        log.info("Processing event for Red Hat advisory {}...", identifier);

        managedExecutor.runAsync(() -> handleAsync(eventRecord.id(), identifier));
    }

    @Transactional(value = TxType.REQUIRES_NEW)
    void handleAsync(String eventId, String advisoryId) {
        // TODO: Let's simulate we are querying ET
        try {
            log.info("Reading advisory {}...", advisoryId);
            Thread.sleep(5000);
            log.info("Advisory {} read", advisoryId);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Event event = Event.findById(eventId);

        if (event == null) {
            log.warn("Event with id '{}' could not be found, cannot schedule generations", eventId);
            return;
        }

        log.info("Creating new generation...");

        // TODO: dummy
        Generation generation = Generation.builder()
                .withId(RandomStringIdGenerator.generate())
                .withIdentifier("DUMMY")
                .withStatus(GenerationStatus.NEW)
                .withType(GenerationRequestType.BUILD.toName())
                .withEvents(List.of(event))
                .build();

        event.getGenerations().add(generation);
    }
}
