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
package org.jboss.sbomer.features.umb.consumer;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.jms.ConnectionFactory;
import javax.jms.Message;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.sbomer.features.umb.UmbConfig;
import org.jboss.sbomer.service.SbomService;

import io.quarkus.arc.Unremovable;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.scheduler.Scheduled;
import io.quarkus.scheduler.Scheduled.SkipPredicate;
import io.quarkus.scheduler.ScheduledExecution;
import lombok.extern.slf4j.Slf4j;

@Unremovable
@ApplicationScoped
@Slf4j
public class UmbMessageConsumer implements MessageConsumer {

    @Singleton
    static class UmbNotEnabledPredicate implements SkipPredicate {

        @Inject
        UmbConfig umbConfig;

        @Override
        public boolean test(ScheduledExecution execution) {
            if (!umbConfig.isEnabled() || !umbConfig.consumer().isEnabled() || umbConfig.consumer().topic().isEmpty()) {
                return true;
            }

            return false;
        }
    }

    @ConfigProperty(name = "quarkus.qpid-jms.url")
    String amqpConnection;

    @Inject
    UmbConfig umbConfig;

    @Inject
    ConnectionFactory connectionFactory;

    @Inject
    SbomService sbomService;

    private final ExecutorService scheduler = Executors.newSingleThreadExecutor();

    @Inject
    PncMessageParser pncMessageParser;

    @Override
    public void init(@Observes StartupEvent ev) {
        if (!umbConfig.isEnabled()) {
            log.info("UMB feature disabled");
            return;
        }

        if (!umbConfig.consumer().isEnabled()) {
            log.info("UMB feature to consume messages disabled");
            return;
        }

        log.info("Initializing connection: {}", amqpConnection);

        if (umbConfig.consumer().topic().isPresent()) {
            scheduler.submit(pncMessageParser);
        }
    }

    @Override
    public void destroy(@Observes ShutdownEvent ev) {
        log.info("Closing connection: {}", amqpConnection);

        pncMessageParser.setShouldRun(false);
        scheduler.shutdown();
    }

    @Override
    public Message getLastMessageReceived() {
        return pncMessageParser.getLastMessage();
    }

    @Override
    public String getMessageReceiverId() {
        return UmbMessageConsumer.class.getName();
    }

    @Scheduled(every = "60s", delay = 30, delayUnit = TimeUnit.SECONDS, skipExecutionIf = UmbNotEnabledPredicate.class)
    public void checkActiveConnection() throws IOException {
        log.info("Checking if UMB connection is active...");

        if (pncMessageParser.shouldRun() && !pncMessageParser.isConnected()) {
            log.info("Reconnecting UMB connection for topic {} ...", umbConfig.consumer().topic().get());
            scheduler.submit(pncMessageParser);
        }
    }

}
