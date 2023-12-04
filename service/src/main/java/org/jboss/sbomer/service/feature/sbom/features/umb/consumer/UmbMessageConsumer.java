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
package org.jboss.sbomer.service.feature.sbom.features.umb.consumer;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.sbomer.service.feature.sbom.config.features.UmbConfig;
import org.jboss.sbomer.service.feature.sbom.service.SbomService;

import io.quarkus.arc.Unremovable;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.scheduler.Scheduled;
import io.quarkus.scheduler.Scheduled.SkipPredicate;
import io.quarkus.scheduler.ScheduledExecution;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
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

    private final Long startupTime = System.currentTimeMillis();

    /**
     * Maximal time without received message. Currently set to 3 hours.
     */
    final private static long MAX_QUIET_TIME_MILLIS = 10800000;

    @ConfigProperty(name = "quarkus.qpid-jms.url")
    Optional<String> amqpConnection;

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

    @Scheduled(every = "1h", delay = 10, delayUnit = TimeUnit.SECONDS, skipExecutionIf = UmbNotEnabledPredicate.class)
    public void checkLastMessageTime() throws IOException, JMSException {
        log.info("Checking if there were UMB messages received recently...");

        if (pncMessageParser.getLastMessage() == null) {
            if (System.currentTimeMillis() - startupTime > MAX_QUIET_TIME_MILLIS) {
                log.warn(
                        "Did not receive any messages since startup for more than {} ms which is the maximal quiet period configured, scheduling consumer restart",
                        MAX_QUIET_TIME_MILLIS);

                // Schedule reconnect
                pncMessageParser.scheduleReconnect();

                return;
            }

            log.debug(
                    "We did not receive any message after startup just yet -- we are running for {} ms, it's OK!",
                    System.currentTimeMillis() - startupTime);
        } else {
            if (System.currentTimeMillis()
                    - pncMessageParser.getLastMessage().getJMSTimestamp() > MAX_QUIET_TIME_MILLIS) {
                log.warn(
                        "Last message was published at {}, it was {} ms ago, this is more than allowed for the quiet period which is currently set at: {} ms, scheduling reconnect",
                        pncMessageParser.getLastMessage().getJMSTimestamp(),
                        System.currentTimeMillis() - pncMessageParser.getLastMessage().getJMSTimestamp(),
                        MAX_QUIET_TIME_MILLIS);

                // Schedule reconnect
                pncMessageParser.scheduleReconnect();

                return;
            }

            log.debug(
                    "We received last message {} ms ago, it's OK!",
                    System.currentTimeMillis() - pncMessageParser.getLastMessage().getJMSTimestamp());
        }

        log.info("UMB consumer seems to be working just fine!");
    }

}
