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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.qpid.jms.provider.exceptions.ProviderConnectionRemotelyClosedException;
import org.jboss.sbomer.service.feature.sbom.config.features.UmbConfig;
import org.jboss.sbomer.service.feature.sbom.config.features.UmbConfig.UmbConsumerTrigger;
import org.jboss.sbomer.service.feature.sbom.features.umb.JmsUtils;
import org.jboss.sbomer.service.feature.sbom.features.umb.consumer.model.PncBuildNotificationMessageBody;

import io.quarkus.arc.Unremovable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.ExceptionListener;
import jakarta.jms.JMSConsumer;
import jakarta.jms.JMSContext;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.Session;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Unremovable
@ApplicationScoped
public class PncMessageParser implements Runnable, ExceptionListener {

    private AtomicBoolean shouldRun = new AtomicBoolean(false);
    private AtomicBoolean connected = new AtomicBoolean(false);
    private AtomicInteger receivedMessages = new AtomicInteger(0);

    @Inject
    UmbConfig config;

    @Inject
    ConnectionFactory cf;

    @Inject
    PncBuildNotificationHandler buildNotificationHandler;

    private Message lastMessage;

    public PncMessageParser() {
        this.shouldRun.set(true);
    }

    @Override
    public void run() {
        if (!config.isEnabled()) {
            log.warn("The UMB feature is disabled");
            return;
        }

        if (!config.consumer().isEnabled()) {
            log.warn("The UMB consumer is disabled");
            return;
        }
        if (config.consumer().topic().isEmpty()) {
            log.warn("Topic not specified, PNC message parser won't run");
            return;
        }

        if (config.consumer().trigger().isPresent() && config.consumer().trigger().get() == UmbConsumerTrigger.NONE) {
            log.warn("The UMB consumer configuration is set to NONE, all builds are skipped");
        }

        log.info("Listening on topic: {}", config.consumer().topic().get());

        try (JMSContext context = cf.createContext(Session.AUTO_ACKNOWLEDGE)) {
            log.info("JMS client ID {}.", context.getClientID());

            // Ensure we catch errors related to the connection.
            context.setExceptionListener(this);

            JMSConsumer consumer = context.createConsumer(context.createQueue(config.consumer().topic().get()));
            while (shouldRun.get()) {
                connected.set(true);
                Message message = consumer.receive();
                if (message == null) {
                    // receive returns `null` if the JMSConsumer is closed
                    return;
                }

                lastMessage = message;
                receivedMessages.incrementAndGet();

                PncBuildNotificationMessageBody msgBody = null;

                try {
                    msgBody = JmsUtils.getMsgBody(lastMessage);
                } catch (JMSException | IOException e) {
                    log.error(
                            "Cannot convert UMB message {} from topic {} to Json",
                            message.getJMSMessageID(),
                            message.getJMSDestination(),
                            e);
                    continue;
                }

                // Handle the message
                buildNotificationHandler.handle(msgBody);
            }
        } catch (Exception e) {
            logFailure(e);
        }

        connected.set(false);
    }

    public boolean shouldRun() {
        return shouldRun.get();
    }

    public void setShouldRun(boolean shouldRun) {
        this.shouldRun.set(shouldRun);
    }

    public boolean isConnected() {
        return connected.get();
    }

    public void scheduleReconnect() {
        connected.set(false);
    }

    public Message getLastMessage() {
        return lastMessage;
    }

    public int getReceivedMessages() {
        return receivedMessages.get();
    }

    private Throwable getRootCause(Throwable throwable) {
        Throwable cause = throwable.getCause();
        while (cause != null && cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause;
    }

    /**
     * A general handler for logging failures related to UMB connection and message consumption. It logs the exception
     * as well.
     *
     * @param e Exception that was thrown by the underlying JMS system.
     */
    public void logFailure(Exception e) {
        Throwable cause = getRootCause(e);

        if (ProviderConnectionRemotelyClosedException.class.equals(cause.getClass())) {
            log.error("The JMS connection was remotely closed, will be handled with the reconnection.", e);
        } else {
            log.error("Something wrong happened in the PNCMessageParser", e);
        }
    }

    @Override
    public void onException(JMSException e) {
        logFailure(e);
        scheduleReconnect();
    }

}
