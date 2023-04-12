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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.jms.ConnectionFactory;
import javax.jms.JMSConsumer;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;

import org.jboss.pnc.api.enums.BuildStatus;
import org.jboss.pnc.api.enums.ProgressStatus;
import org.jboss.pnc.common.Strings;
import org.jboss.sbomer.core.enums.GeneratorImplementation;
import org.jboss.sbomer.service.SbomService;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PNCMessageParser implements Runnable {

    private static final ObjectMapper msgMapper = new ObjectMapper();

    static {
        msgMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        msgMapper.registerModule(new JavaTimeModule()).configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        msgMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        msgMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        msgMapper.disable(SerializationFeature.FAIL_ON_UNWRAPPED_TYPE_IDENTIFIERS);
    }

    private AtomicBoolean shouldRun = new AtomicBoolean(false);
    private AtomicBoolean connected = new AtomicBoolean(false);
    private AtomicInteger receivedMessages = new AtomicInteger(0);
    private ConnectionFactory cf;
    private SbomService sbomService;
    private boolean generateSboms;
    private String incomingTopic;
    private Message lastMessage;

    public PNCMessageParser(
            ConnectionFactory cf,
            String incomingTopic,
            SbomService sbomService,
            boolean generateSboms) {
        this.cf = cf;
        this.incomingTopic = incomingTopic;
        this.sbomService = sbomService;
        this.generateSboms = generateSboms;
        this.shouldRun.set(true);
    }

    @Override
    public void run() {
        log.info("Listening on topic: {}", incomingTopic);

        try (JMSContext context = cf.createContext(Session.AUTO_ACKNOWLEDGE)) {
            log.info("JMS client ID {}.", context.getClientID());

            JMSConsumer consumer = context.createConsumer(context.createQueue(incomingTopic));
            while (shouldRun.get()) {
                connected.set(true);
                Message message = consumer.receive();
                if (message == null) {
                    // receive returns `null` if the JMSConsumer is closed
                    return;
                }

                lastMessage = message;
                receivedMessages.incrementAndGet();

                try {

                    JsonNode msgBody = JmsUtils.getMsgBody(lastMessage);
                    if (msgBody == null) {
                        continue;
                    }
                    if (generateSboms && isSuccessfulPersistentBuild(msgBody)) {
                        String buildId = msgBody.path("build").path("id").asText();
                        if (!Strings.isEmpty(buildId)) {
                            log.info("I SHOULD REALLY GENERATE AN SBOM FOR BUILD {}", buildId);
                            sbomService.generate(buildId, GeneratorImplementation.CYCLONEDX);
                        }
                    }
                } catch (JMSException | IOException e) {
                    log.error(
                            "Cannot convert UMB message {} from topic {} to Json",
                            message.getJMSMessageID(),
                            message.getJMSDestination(),
                            e);
                }

            }
        } catch (Exception e) {
            log.error("Something wrong happened in the PNCMessageParser", e);
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

    public Message getLastMessage() {
        return lastMessage;
    }

    public int getReceivedMessages() {
        return receivedMessages.get();
    }

    public boolean isSuccessfulPersistentBuild(JsonNode msgBody) {

        JsonNode buildNode = msgBody.path("build");
        Boolean persistent = !buildNode.path("temporaryBuild").asBoolean();
        String status = buildNode.path("status").asText();
        String progress = msgBody.path("build").path("progress").asText();

        if (persistent && ProgressStatus.FINISHED.name().equals(progress) && (BuildStatus.SUCCESS.name().equals(status)
                || BuildStatus.NO_REBUILD_REQUIRED.name().equals(status))) {
            return true;
        }

        return false;
    }

}
