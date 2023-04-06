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
package org.jboss.sbomer.test.messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.jms.ConnectionFactory;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.TextMessage;

import org.jboss.pnc.api.enums.BuildStatus;
import org.jboss.pnc.api.enums.ProgressStatus;
import org.jboss.sbomer.core.test.TestResources;
import org.jboss.sbomer.messaging.JmsUtils;
import org.jboss.sbomer.messaging.UmbMessageConsumer;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import com.fasterxml.jackson.databind.JsonNode;

import io.quarkus.artemis.test.ArtemisTestResource;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import lombok.extern.slf4j.Slf4j;

@QuarkusTest
@QuarkusTestResource(ArtemisTestResource.class)
@TestMethodOrder(OrderAnnotation.class)
@Slf4j
public class PncBuildTest {

    @Inject
    UmbMessageConsumer msgConsumer;

    @Inject
    ConnectionFactory connectionFactory;

    @Test
    @Order(1)
    public void testUMBProducerPNCBuild() throws Exception {
        log.info("Running testUMBProducerPNCBuild...");

        try (JMSContext context = connectionFactory.createContext(JMSContext.AUTO_ACKNOWLEDGE)) {
            TextMessage txgMsg = preparePNCBuildMsg(context);
            context.createProducer()
                    .send(context.createQueue("Consumer.pncsbomer.testing.VirtualTopic.eng.pnc.>"), txgMsg);
        }
    }

    @Test
    @Order(2)
    public void testUMBConsumerPNCBuild() throws Exception {
        log.info("Running testUMBConsumerPNCBuild...");

        assertTrue(Wait.waitFor(() -> {
            Message msg = msgConsumer.getLastMessageReceived();
            return msg != null;
        }, 10000, 25), "PNC Build message did not became available in allotted time");

        Message msg = msgConsumer.getLastMessageReceived();
        String producer = msg.getStringProperty("producer");
        assertEquals("PNC", producer);

        JsonNode msgBody = JmsUtils.getMsgBody(msg);
        JsonNode buildNode = msgBody.path("build");

        Boolean persistent = !buildNode.path("temporaryBuild").asBoolean();
        String status = buildNode.path("status").asText();
        String progress = msgBody.path("build").path("progress").asText();
        String buildId = msgBody.path("build").path("id").asText();

        assertEquals(true, persistent);
        assertEquals(BuildStatus.SUCCESS.name(), status);
        assertEquals(ProgressStatus.FINISHED.name(), progress);
        assertEquals("AX5TJMYHQAIAE", buildId);

        assertTrue(
                persistent && ProgressStatus.FINISHED.name().equals(progress)
                        && (BuildStatus.SUCCESS.name().equals(status)
                                || BuildStatus.NO_REBUILD_REQUIRED.name().equals(status)));

    }

    private TextMessage preparePNCBuildMsg(JMSContext context) throws IOException {

        // Rebuilding mesg
        // https://datagrepper.engineering.redhat.com/id?id=ID:orch-70-z4tx6-43917-1678809685060-25:1:8557:1:1&is_raw=true&size=extra-large
        Map<String, String> headers = new HashMap<>();
        headers.put("type", "BuildStateChange");
        headers.put("attribute", "state-change");
        headers.put("name", "org.kie-kie-jpmml-integration-7.67.0.Final-7.13.3");
        headers.put("configurationId", "10884");
        headers.put("configurationRevision", "2653483");
        headers.put("oldStatus", "BUILDING");
        headers.put("newStatus", "SUCCESS");
        headers.put("JMSXUserID", "projectnewcastle");
        headers.put("amq6100_originalDestination", "topic://VirtualTopic.eng.pnc.builds");
        headers.put("correlationId", "a420416c-d184-4ced-9277-500667305139");
        headers.put("destination", "/topic/VirtualTopic.eng.pnc.builds");
        headers.put("messageId", "ID:orch-70-z4tx6-43917-1678809685060-25:1:8557:1:1");
        headers.put("persistent", "true");
        headers.put("producer", "PNC");

        String msgBody = TestResources.asString("payloads/umb-pnc-build-body.json");
        TextMessage textMessage = context.createTextMessage(msgBody);

        headers.forEach((k, v) -> {
            try {
                textMessage.setStringProperty(k, v);
            } catch (JMSException e) {
                log.error("Error while setting JMS headers", e);
            }
        });

        return textMessage;
    }

}