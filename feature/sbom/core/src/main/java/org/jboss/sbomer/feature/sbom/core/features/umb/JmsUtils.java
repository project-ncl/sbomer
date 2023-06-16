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
package org.jboss.sbomer.feature.sbom.core.features.umb;

import java.io.IOException;
import java.util.Enumeration;

import javax.jms.BytesMessage;
import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.ObjectMessage;
import javax.jms.TextMessage;

import org.jboss.pnc.common.json.JsonUtils;
import org.jboss.sbomer.feature.sbom.core.features.umb.consumer.model.PncBuildNotificationMessageBody;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JmsUtils {

    public static final ObjectMapper msgMapper = new ObjectMapper();

    static {
        msgMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        msgMapper.registerModule(new JavaTimeModule()).configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        msgMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        msgMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        msgMapper.disable(SerializationFeature.FAIL_ON_UNWRAPPED_TYPE_IDENTIFIERS);
    }

    public static PncBuildNotificationMessageBody getMsgBody(Message message) throws JMSException, IOException {
        if (message == null) {
            return null;
        }
        try {
            if (message instanceof TextMessage) {

                String textMessage = message.getBody(String.class);
                return msgMapper.readValue(textMessage, PncBuildNotificationMessageBody.class);

            } else if (message instanceof BytesMessage) {

                byte[] bytesMessage = message.getBody(byte[].class);
                return msgMapper.readValue(bytesMessage, PncBuildNotificationMessageBody.class);

            } else if (message instanceof ObjectMessage) {

                Object objectMessage = message.getBody(Object.class);
                return msgMapper.readValue(JsonUtils.toJson(objectMessage), PncBuildNotificationMessageBody.class);

            } else if (message instanceof MapMessage) {

                MapMessage mm = (MapMessage) message;
                ObjectNode root = msgMapper.createObjectNode();
                Enumeration<String> e = mm.getMapNames();
                while (e.hasMoreElements()) {
                    String field = e.nextElement();
                    root.set(field, msgMapper.convertValue(mm.getObject(field), JsonNode.class));
                }

                return msgMapper.readValue(
                        msgMapper.writerWithDefaultPrettyPrinter().writeValueAsString(root),
                        PncBuildNotificationMessageBody.class);
            }
            return null;
        } catch (JMSException | IOException exc) {
            log.error("Could not parse body of last received message, ", exc);
            throw exc;
        }
    }
}
