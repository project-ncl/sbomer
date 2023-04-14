/**
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
package org.jboss.sbomer.messaging;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import javax.jms.ConnectionFactory;
import javax.jms.Message;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.pnc.common.Strings;
import org.jboss.sbomer.service.SbomService;

import io.quarkus.arc.Unremovable;
import io.quarkus.scheduler.Scheduled;
import lombok.extern.slf4j.Slf4j;

import javax.enterprise.event.Observes;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;

@Unremovable
@ApplicationScoped
@Slf4j
public class UmbMessageConsumer implements MessageConsumer {

    @ConfigProperty(name = "quarkus.qpid-jms.url")
    String amqpConnection;

    @ConfigProperty(name = "sbomer.umb.consumers.topic")
    String topic;

    @ConfigProperty(name = "sbomer.umb.consumers.enabled")
    boolean enabled;

    @ConfigProperty(name = "sbomer.umb.consumers.trigger-sbom-generation")
    boolean generateSboms;

    @Inject
    ConnectionFactory connectionFactory;

    @Inject
    SbomService sbomService;

    private final ExecutorService scheduler = Executors.newSingleThreadExecutor();

    private PNCMessageParser pncMessageParser;

    @Override
    public void init(@Observes StartupEvent ev) {
        log.info("Initializing connection: {}", amqpConnection);
        if (!Strings.isEmpty(topic) && enabled) {
            pncMessageParser = new PNCMessageParser(connectionFactory, topic, sbomService, generateSboms);
            scheduler.submit(pncMessageParser);
        }
    }

    @Override
    public void destroy(@Observes ShutdownEvent ev) {
        if (pncMessageParser != null) {
            pncMessageParser.setShouldRun(false);
        }
        scheduler.shutdown();
    }

    @Override
    public Message getLastMessageReceived() {
        if (pncMessageParser != null) {
            return pncMessageParser.getLastMessage();
        }
        return null;
    }

    @Override
    public String getMessageReceiverId() {
        return UmbMessageConsumer.class.getName();
    }

    @Scheduled(every = "60s", delay = 30, delayUnit = TimeUnit.SECONDS)
    public void checkActiveConnection() throws IOException {
        log.info("Checking if UMB connection is active...");

        if (enabled && pncMessageParser != null && pncMessageParser.shouldRun() && !pncMessageParser.isConnected()) {
            log.info("Reconnecting UMB connection for topic {} ...", topic);

            pncMessageParser = new PNCMessageParser(connectionFactory, topic, sbomService, generateSboms);
            scheduler.submit(pncMessageParser);
        }
    }

}
