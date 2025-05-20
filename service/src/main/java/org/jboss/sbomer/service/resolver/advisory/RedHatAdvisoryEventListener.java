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
package org.jboss.sbomer.service.resolver.advisory;

import java.util.List;

import org.eclipse.microprofile.context.ManagedExecutor;
import org.jboss.sbomer.core.features.sbom.enums.GenerationRequestType;
import org.jboss.sbomer.service.feature.sbom.model.RandomStringIdGenerator;
import org.jboss.sbomer.service.feature.sbom.model.v1beta2.Event;
import org.jboss.sbomer.service.feature.sbom.model.v1beta2.Generation;
import org.jboss.sbomer.service.feature.sbom.model.v1beta2.enums.GenerationStatus;
import org.jboss.sbomer.service.resolver.AbstractResolver;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;
import lombok.extern.slf4j.Slf4j;

@ApplicationScoped
@Slf4j
public class RedHatAdvisoryEventListener extends AbstractResolver {

    public static final String RESOLVER_TYPE = "rh-advisory";

    public RedHatAdvisoryEventListener() {

    }

    @Inject
    public RedHatAdvisoryEventListener(ManagedExecutor managedExecutor) {
        super(managedExecutor);
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

    public void resolveAdvisory(String advisoryId) {
        // TODO: Let's simulate we are querying ET
        try {
            log.info("Reading advisory {}...", advisoryId);
            Thread.sleep(5000);
            log.info("Advisory {} read", advisoryId);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
