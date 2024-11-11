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

import static org.jboss.sbomer.core.features.sbom.enums.UMBConsumer.ERRATA;
import static org.jboss.sbomer.core.features.sbom.enums.UMBConsumer.PNC;
import static org.jboss.sbomer.core.features.sbom.enums.UMBMessageStatus.ACK;
import static org.jboss.sbomer.core.features.sbom.enums.UMBMessageStatus.SKIPPED;
import static org.jboss.sbomer.service.feature.sbom.model.RequestEvent.EVENT_KEY_UMB_CONSUMER;
import static org.jboss.sbomer.service.feature.sbom.model.RequestEvent.EVENT_KEY_UMB_MSG_ID;
import static org.jboss.sbomer.service.feature.sbom.model.RequestEvent.EVENT_KEY_UMB_MSG_STATUS;
import static org.jboss.sbomer.service.feature.sbom.model.RequestEvent.EVENT_KEY_UMB_MSG_TYPE;
import static org.jboss.sbomer.service.feature.sbom.model.RequestEvent.EVENT_VALUE_UMB_UNKNOWN_MSG_TYPE;
import static org.jboss.sbomer.service.feature.sbom.model.RequestEvent.REQUEST_CONFIG_TYPE;
import static org.jboss.sbomer.service.feature.sbom.model.RequestEvent.REQUEST_EVENT_TYPE;
import static org.jboss.sbomer.service.feature.sbom.model.RequestEventType.UMB;

import java.lang.reflect.Field;
import java.util.Map;

import org.jboss.sbomer.core.config.request.RequestConfig;
import org.jboss.sbomer.core.features.sbom.enums.UMBConsumer;
import org.jboss.sbomer.core.features.sbom.enums.UMBMessageStatus;
import org.jboss.sbomer.service.feature.sbom.model.RequestEvent;
import org.jboss.sbomer.service.feature.sbom.model.RequestEventType;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.Query;

@ApplicationScoped
public class RequestEventRepository implements PanacheRepository<RequestEvent> {

    private static final String BASE_COUNT_QUERY = "SELECT COUNT(*) FROM request";
    private static final String BASE_SELECT_QUERY = "SELECT * FROM request";

    public long countUMBEventsWithStatusFrom(UMBMessageStatus status, UMBConsumer consumer) {
        StringBuilder query = initCountRequestQuery();
        addCondition(query, "WHERE", REQUEST_EVENT_TYPE, "=");
        addEventCondition(query, "AND", EVENT_KEY_UMB_CONSUMER, "=");
        addEventCondition(query, "AND", EVENT_KEY_UMB_MSG_STATUS, "=");
        addEventCondition(query, "AND", EVENT_KEY_UMB_MSG_TYPE, "<>");

        Map<String, Object> params = Map.of(
                REQUEST_EVENT_TYPE,
                UMB.name(),
                EVENT_KEY_UMB_CONSUMER,
                consumer.name(),
                EVENT_KEY_UMB_MSG_TYPE,
                EVENT_VALUE_UMB_UNKNOWN_MSG_TYPE,
                EVENT_KEY_UMB_MSG_STATUS,
                status.name());

        return executeCountQuery(query.toString(), params);
    }

    public long countAllUMBEventsFrom(UMBConsumer consumer) {
        StringBuilder query = initCountRequestQuery();
        addCondition(query, "WHERE", REQUEST_EVENT_TYPE, "=");
        addEventCondition(query, "AND", EVENT_KEY_UMB_CONSUMER, "=");

        Map<String, Object> params = Map.of(REQUEST_EVENT_TYPE, UMB.name(), EVENT_KEY_UMB_CONSUMER, consumer.name());

        return executeCountQuery(query.toString(), params);
    }

    public long countAlreadyAckedUMBEventsFor(String msgId) {
        StringBuilder query = initCountRequestQuery();
        addCondition(query, "WHERE", REQUEST_EVENT_TYPE, "=");
        addEventCondition(query, "AND", EVENT_KEY_UMB_MSG_STATUS, "=");
        addEventCondition(query, "AND", EVENT_KEY_UMB_MSG_ID, "=");

        Map<String, Object> params = Map
                .of(REQUEST_EVENT_TYPE, UMB.name(), EVENT_KEY_UMB_MSG_STATUS, ACK.name(), EVENT_KEY_UMB_MSG_ID, msgId);

        return executeCountQuery(query.toString(), params);
    }

    public long countEventsForTypeAndIdentifier(String typeValue, String identifierKey, String identifierValue) {
        StringBuilder query = initCountRequestQuery();
        addConfigCondition(query, "WHERE", REQUEST_CONFIG_TYPE, "=");
        addConfigCondition(query, "AND", identifierKey, "=");

        Map<String, Object> params = Map.of(REQUEST_CONFIG_TYPE, typeValue, identifierKey, identifierValue);

        return executeCountQuery(query.toString(), params);
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
        return countUMBEventsWithStatusFrom(ACK, ERRATA);
    }

    public long countErrataSkippedMessages() {
        return countUMBEventsWithStatusFrom(SKIPPED, ERRATA);
    }

    public long countPncProcessedMessages() {
        return countUMBEventsWithStatusFrom(ACK, PNC);
    }

    public long countPncSkippedMessages() {
        return countUMBEventsWithStatusFrom(SKIPPED, PNC);
    }

    public long countPncReceivedMessages() {
        return countAllUMBEventsFrom(PNC);
    }

    public long countErrataReceivedMessages() {
        return countAllUMBEventsFrom(ERRATA);
    }

    protected StringBuilder initCountRequestQuery() {
        return new StringBuilder(BASE_COUNT_QUERY);
    }

    protected StringBuilder initSelectRequestQuery() {
        return new StringBuilder(BASE_SELECT_QUERY);
    }

    protected StringBuilder addCondition(StringBuilder query, String condition, String property, String operator) {
        query.append(" ")
                .append(condition)
                .append(" ")
                .append(property)
                .append(" ")
                .append(operator)
                .append(" :")
                .append(property);
        return query;
    }

    protected StringBuilder addEventCondition(StringBuilder query, String condition, String property, String operator) {
        query.append(" ")
                .append(condition)
                .append(" event ->> '")
                .append(property)
                .append("' ")
                .append(operator)
                .append(" :")
                .append(property);
        return query;
    }

    protected StringBuilder addConfigCondition(
            StringBuilder query,
            String condition,
            String property,
            String operator) {
        query.append(" ")
                .append(condition)
                .append(" request_config ->> '")
                .append(property)
                .append("' ")
                .append(operator)
                .append(" :")
                .append(property);
        return query;
    }

    protected long executeCountQuery(String query, Map<String, Object> params) {
        Query q = getEntityManager().createNativeQuery(query);
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            q.setParameter(entry.getKey(), entry.getValue());
        }
        return ((Number) q.getSingleResult()).longValue();
    }

    private String getValueOfField(Class<? extends RequestConfig> configClass, String fieldName) {
        try {
            Field field = configClass.getDeclaredField(fieldName);
            return (String) field.get(null);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new IllegalArgumentException(
                    "The class " + configClass.getName() + " does not have a public static final " + fieldName
                            + " field.",
                    e);
        }
    }

}