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
package org.jboss.sbomer.features.umb.producer;

import java.util.HashMap;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.sbomer.core.utils.Constants;
import org.jboss.sbomer.features.umb.UmbConfig;
import org.jboss.sbomer.features.umb.producer.model.GenerationFinishedMessageBody;

import io.quarkus.arc.Unremovable;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import lombok.extern.slf4j.Slf4j;

@Unremovable
@ApplicationScoped
@Slf4j
public class UmbMessageProducer implements MessageProducer {

    public static final String MESSAGE_HEADER_PURL_KEY = "purl";
    public static final String MESSAGE_HEADER_BUILD_ID_KEY = Constants.SBOM_RED_HAT_BUILD_ID;
    public static final String MESSAGE_HEADER_SBOM_ID_KEY = "sbom-id";
    public static final String MESSAGE_HEADER_PRODUCER_KEY = "producer";
    public static final String MESSAGE_HEADER_TYPE_KEY = "type";

    @ConfigProperty(name = "quarkus.qpid-jms.url")
    String amqpConnection;

    @Inject
    UmbConfig umbConfig;

    @Inject
    ConnectionFactory cf;

    Connection connection;

    @Override
    public void init(@Observes StartupEvent ev) {
        if (!umbConfig.isEnabled()) {
            log.info("UMB feature disabled");
            return;
        }

        if (!umbConfig.producer().isEnabled()) {
            log.info("UMB feature to produce notification messages disabled");
            return;
        }

        if (!umbConfig.producer().topic().isPresent()) {
            log.info("UMB producer topic not specified");
            return;
        }

        initConnection();
    }

    @Override
    public void destroy(@Observes ShutdownEvent ev) {
        closeConnection();
    }

    @Override
    public String getMessageSenderId() {
        return UmbMessageProducer.class.getName();
    }

    @Override
    public void sendToTopic(GenerationFinishedMessageBody message) {
        Map<String, String> headers = new HashMap<String, String>();
        headers.put(MESSAGE_HEADER_PURL_KEY, message.getPurl());
        headers.put(MESSAGE_HEADER_BUILD_ID_KEY, message.getBuild().getId());
        headers.put(MESSAGE_HEADER_SBOM_ID_KEY, message.getSbom().getId());
        headers.put(MESSAGE_HEADER_PRODUCER_KEY, "PNC SBOMer");
        headers.put(MESSAGE_HEADER_TYPE_KEY, "GenerationFinishedMessage");
        sendMessageWithRetries(message.toJson(), headers, umbConfig.producer().retries());
    }

    private void sendMessageWithRetries(String message, Map<String, String> headers, int retries) {
        Session session = null;
        javax.jms.MessageProducer messageProducer = null;
        try {
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            messageProducer = session.createProducer(session.createQueue(umbConfig.producer().topic().get()));
            sendMessage(message, headers, session, messageProducer);
        } catch (Exception e) {
            if (retries <= 1) {
                // give up
                throw new RuntimeException(
                        "Cannot send the message: " + message + "; with headers: " + headers + ".",
                        e);
            } else {
                sleep(retries);
                // try to set up a new connection on exception for the next message
                initConnection();
                sendMessageWithRetries(message, headers, retries - 1);
            }
        } finally {
            closeSession(session);
            closeMsgProducer(messageProducer);
        }
    }

    private void sendMessage(
            String message,
            Map<String, String> headers,
            Session session,
            javax.jms.MessageProducer messageProducer) {

        TextMessage textMessage;
        try {
            textMessage = session.createTextMessage(message);
        } catch (JMSException e) {
            log.error("Unable to create textMessage.");
            throw new RuntimeException("Unable to create textMessage.", e);
        }

        StringBuilder headerBuilder = new StringBuilder();
        headers.forEach((k, v) -> {
            try {
                textMessage.setStringProperty(k, v);
                headerBuilder.append(k + ":" + v + "; ");
            } catch (JMSException e) {
                throw new RuntimeException(e);
            }
        });
        try {
            log.debug("Sending message with headers: {}.", headerBuilder.toString());
            messageProducer.send(textMessage);
        } catch (JMSException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Sleep exponentially as the number of retries decrease, up till max back-off time of configured seconds
     *
     * @param retries
     */
    private void sleep(int retries) {
        int sleepMilli = (int) Math.min(Math.pow(2, (double) 10 / retries) * 10, umbConfig.producer().maxBackOff());
        try {
            Thread.sleep(sleepMilli);
        } catch (InterruptedException e) {
            log.warn("Sleeping was interrupted", e);
        }
    }

    private void initConnection() {
        try {
            connection = cf.createConnection();
            log.info("JMS client ID {}.", connection.getClientID());
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize JMS.", e);
        }
    }

    private void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
            } catch (Exception e) {
                log.error("Failed to close JMS connection.", e);
            }
        }
    }

    private void closeSession(Session session) {
        if (session != null) {
            try {
                session.close();
            } catch (JMSException e) {
                log.error("Cannot close JMS session.");
            }
        }
    }

    private void closeMsgProducer(javax.jms.MessageProducer messageProducer) {
        if (messageProducer != null) {
            try {
                messageProducer.close();
            } catch (JMSException e) {
                log.error("Cannot close JMS messageProducer.");
            }
        }
    }

}
