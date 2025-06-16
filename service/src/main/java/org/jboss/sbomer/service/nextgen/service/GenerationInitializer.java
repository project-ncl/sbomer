package org.jboss.sbomer.service.nextgen.service;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.microprofile.context.ManagedExecutor;
import org.jboss.sbomer.core.errors.ApplicationException;
import org.jboss.sbomer.service.nextgen.core.dto.model.EventRecord;
import org.jboss.sbomer.service.nextgen.core.enums.EventStatus;
import org.jboss.sbomer.service.nextgen.core.events.EventStatusChangeEvent;
import org.jboss.sbomer.service.nextgen.core.payloads.generation.GenerationRequestSpec;
import org.jboss.sbomer.service.nextgen.core.payloads.generation.GenerationsRequest;
import org.jboss.sbomer.service.nextgen.core.utils.JacksonUtils;
import org.jboss.sbomer.service.nextgen.service.config.GeneratorConfigProvider;
import org.jboss.sbomer.service.nextgen.service.model.Event;
import org.jboss.sbomer.service.nextgen.service.model.Generation;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.quarkus.arc.Arc;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.TransactionPhase;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;
import lombok.extern.slf4j.Slf4j;

@ApplicationScoped
@Slf4j
public class GenerationInitializer {

    GeneratorConfigProvider generatorConfigProvider;
    ManagedExecutor managedExecutor;
    EntityMapper mapper;

    @Inject
    public GenerationInitializer(
            EntityMapper mapper,
            ManagedExecutor managedExecutor,
            GeneratorConfigProvider generatorConfigProvider) {
        this.managedExecutor = managedExecutor;
        this.generatorConfigProvider = generatorConfigProvider;
        this.mapper = mapper;
    }

    public void onEvent(@Observes(during = TransactionPhase.AFTER_SUCCESS) EventStatusChangeEvent event) {
        if (event.event().status() != EventStatus.INITIALIZING) {
            return;
        }

        managedExecutor.runAsync(() -> {
            try {
                // TODO: update status

                List<Generation> generations = initGenerations(event.event());

                save(event.event().id(), generations);
            } catch (Exception e) {
                log.error("Unable to generate", e);

                // TODO: update status
            }
        });
    }

    List<Generation> initGenerations(EventRecord eventRecord) {
        List<Generation> generations = new ArrayList<>();

        // TODO: can we make it safer?
        GenerationsRequest generationsRequest = JacksonUtils
                .parse(GenerationsRequest.class, (ObjectNode) eventRecord.request());

        generationsRequest.requests().forEach(request -> {
            log.debug("Processing request: '{}'", request.target());

            // Crucial step. From a request, create an effective config which selects the appropriate generator and
            // prepares its config.
            GenerationRequestSpec effectiveRequest = generatorConfigProvider.buildEffectiveRequest(request);

            log.debug("Effective request: '{}'", effectiveRequest);

            Generation generation = Generation.builder()
                    .withRequest(JacksonUtils.toObjectNode(effectiveRequest))
                    .withReason("Created as a result of a REST API call")
                    .build();

            generations.add(generation);
        });

        return generations;

    }

    @Transactional(value = TxType.REQUIRES_NEW)
    public void save(String eventId, List<Generation> generations) {
        Event event = Event.findById(eventId);

        if (event == null) {
            throw new ApplicationException(
                    "Unable to find Event with id '{}', processing this request cannot continue. Make sure you either: provide correct event id or remove it entirely.",
                    eventId);

            // TODO: update status!
        }

        event.setStatus(EventStatus.INITIALIZED);
        event.setReason("Event initialized");
        event.getGenerations().addAll(generations);
        event.save();

        Arc.container().beanManager().getEvent().fire(new EventStatusChangeEvent(mapper.toRecord(event)));
    }

}
