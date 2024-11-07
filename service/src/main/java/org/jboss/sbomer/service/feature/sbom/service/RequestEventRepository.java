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
package org.jboss.sbomer.service.feature.sbom.service;

import java.lang.reflect.Field;

import org.jboss.sbomer.core.config.request.RequestConfig;
import org.jboss.sbomer.core.features.sbom.enums.UMBConsumer;
import org.jboss.sbomer.core.features.sbom.enums.UMBMessageStatus;
import org.jboss.sbomer.service.feature.sbom.model.RequestEvent;
import org.jboss.sbomer.service.feature.sbom.model.RequestEventType;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class RequestEventRepository implements PanacheRepository<RequestEvent> {

    public long countAckedUMBEventsFrom(UMBConsumer consumer) {
        String q = "SELECT COUNT(*) FROM request WHERE event_type = :event_type AND event ->> 'consumer' = :consumer "
                + " AND event ->> 'msg_type' <> :msg_type " + " AND event ->> 'msg_status' = :msg_status";

        return ((Number) getEntityManager().createNativeQuery(q)
                .setParameter(RequestEvent.REQUEST_EVENT_TYPE, RequestEventType.UMB.name())
                .setParameter(RequestEvent.EVENT_KEY_UMB_CONSUMER, consumer.name())
                .setParameter(RequestEvent.EVENT_KEY_UMB_MSG_TYPE, RequestEvent.EVENT_VALUE_UMB_UNKNOWN_MSG_TYPE)
                .setParameter(RequestEvent.EVENT_KEY_UMB_MSG_STATUS, UMBMessageStatus.ACK.name())
                .getSingleResult()).longValue();
    }

    public long countAllUMBEventsFrom(UMBConsumer consumer) {
        String q = "SELECT COUNT(*) FROM request WHERE event_type = :event_type "
                + " AND event ->> 'consumer' = :consumer";

        return ((Number) getEntityManager().createNativeQuery(q)
                .setParameter(RequestEvent.REQUEST_EVENT_TYPE, RequestEventType.UMB.name())
                .setParameter(RequestEvent.EVENT_KEY_UMB_CONSUMER, consumer.name())
                .getSingleResult()).longValue();
    }

    public long countEventsForTypeAndIdentifier(String typeValue, String identifierKey, String identifierValue) {

        String q = "SELECT COUNT(*) FROM request WHERE request_config ->> 'type' = :type " + " AND request_config ->> '"
                + identifierKey + "' = :identifierValue";

        return ((Number) getEntityManager().createNativeQuery(q)
                .setParameter("type", typeValue)
                .setParameter("identifierValue", identifierValue)
                .getSingleResult()).longValue();
    }

    public long countEventsForConfigWithIdentifierValue(
            Class<? extends RequestConfig> configClass,
            String identifierValue) {
        String typeName = getValueOfField(configClass, "TYPE_NAME");
        String identifierKey = getValueOfField(configClass, "IDENTIFIER_KEY");

        return countEventsForTypeAndIdentifier(typeName, identifierKey, identifierValue);
    }

    public long countAllEventsOfType(RequestEventType eventType) {
        return count("eventType = ?1", eventType);
    }

    public long countErrataProcessedMessages() {
        return countAckedUMBEventsFrom(UMBConsumer.ERRATA);
    }

    public long countPncProcessedMessages() {
        return countAckedUMBEventsFrom(UMBConsumer.PNC);
    }

    public long countPncReceivedMessages() {
        return countAllUMBEventsFrom(UMBConsumer.PNC);
    }

    public long countErrataReceivedMessages() {
        return countAllUMBEventsFrom(UMBConsumer.ERRATA);
    }

    private String getValueOfField(Class<? extends RequestConfig> configClass, String fieldName) {
        // Use reflection to get the value of the field provided
        try {
            Field typeNameField = configClass.getDeclaredField(fieldName);
            return (String) typeNameField.get(null);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new IllegalArgumentException(
                    "The class " + configClass.getName() + " does not have a public static final " + fieldName
                            + " field.",
                    e);
        }
    }

}