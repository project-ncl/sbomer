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
package org.jboss.sbomer.service.v1beta2.resolver.advisory;

import java.util.List;

import org.eclipse.microprofile.context.ManagedExecutor;
import org.jboss.sbomer.core.utils.ObjectMapperUtils;
import org.jboss.sbomer.service.feature.sbom.model.v1beta2.Event;
import org.jboss.sbomer.service.feature.sbom.model.v1beta2.Generation;
import org.jboss.sbomer.service.feature.sbom.model.v1beta2.enums.EventStatus;
import org.jboss.sbomer.service.feature.sbom.model.v1beta2.enums.GenerationStatus;
import org.jboss.sbomer.service.rest.api.v1beta2.payloads.generation.GenerationRequestSpec;
import org.jboss.sbomer.service.rest.api.v1beta2.payloads.generation.TargetSpec;
import org.jboss.sbomer.service.v1beta2.generator.GeneratorConfigProvider;
import org.jboss.sbomer.service.v1beta2.resolver.AbstractResolver;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@ApplicationScoped
@NoArgsConstructor
@Slf4j
public class ErrataToolAdvisoryResolver extends AbstractResolver {

    public static final String RESOLVER_TYPE = "et-advisory";

    GeneratorConfigProvider generatorConfigProvider;

    @Inject
    public ErrataToolAdvisoryResolver(
            ManagedExecutor managedExecutor,
            GeneratorConfigProvider generatorConfigProvider) {
        super(managedExecutor);
        this.generatorConfigProvider = generatorConfigProvider;
    }

    @Override
    public String getType() {
        return RESOLVER_TYPE;
    }

    @Override
    public void resolve(String eventId, String advisoryId) {
        resolveAdvisory(advisoryId);
        scheduleGenerations(eventId);
    }

    @Transactional(value = TxType.REQUIRES_NEW)
    void scheduleGenerations(String eventId) {
        log.info("Creating new generation...");

        Event event = Event.findById(eventId);

        if (event == null) {
            log.warn("Event with id '{}' could not be found, cannot schedule generations", eventId);
            return;
        }
        GenerationRequestSpec dummyRequestSpec = new GenerationRequestSpec(
                new TargetSpec("quay.io/pct-security/mequal:latest", "CONTAINER_IMAGE"),
                null);

        GenerationRequestSpec effectiveRequest = generatorConfigProvider.buildEffectiveRequest(dummyRequestSpec);

        // TODO: dummy
        Generation generation = Generation.builder()
                .withEvents(List.of(event))
                // Convert payload to JsonNode
                .withRequest(ObjectMapperUtils.toJsonNode(effectiveRequest))
                .build()
                .save();

        // Create status update
        generation.updateStatus(GenerationStatus.NEW, "Generation created");

        event.getGenerations().add(generation);
        event.updateStatus(EventStatus.RESOLVED, "Event was successfully resolved");
    }

    public void resolveAdvisory(String advisoryId) {
        // TODO: Let's simulate we are querying ET
        try {
            log.info("Reading advisory {}...", advisoryId);
            Thread.sleep(15000);
            log.info("Advisory {} read", advisoryId);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
