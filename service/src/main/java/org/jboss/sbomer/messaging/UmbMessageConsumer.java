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
import org.jboss.sbomer.service.SBOMService;

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

    @ConfigProperty(name = "sbomer.umb.pnc-builds-topic")
    String pncUmbIncomingTopic;

    @ConfigProperty(name = "quarkus.qpid-jms.url")
    String amqpConnection;

    @Inject
    ConnectionFactory connectionFactory;

    @Inject
    SBOMService sbomService;

    private final ExecutorService scheduler = Executors.newSingleThreadExecutor();

    private PNCMessageParser pncMessageParser;

    @Override
    public void init(@Observes StartupEvent ev) {
        log.info("Initializing connection: {}", amqpConnection);
        if (!Strings.isEmpty(pncUmbIncomingTopic)) {
            pncMessageParser = new PNCMessageParser(connectionFactory, pncUmbIncomingTopic, sbomService);
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

        if (pncMessageParser != null && pncMessageParser.shouldRun() && !pncMessageParser.isConnected()) {
            log.info("Reconnecting UMB connection for topic {} ...", pncUmbIncomingTopic);

            pncMessageParser = new PNCMessageParser(connectionFactory, pncUmbIncomingTopic, sbomService);
            scheduler.submit(pncMessageParser);
        }
    }

}
