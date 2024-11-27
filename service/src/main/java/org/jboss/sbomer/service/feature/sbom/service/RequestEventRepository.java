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

import static org.jboss.sbomer.core.features.sbom.enums.RequestEventType.UMB;
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

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.sbomer.core.config.request.ErrataAdvisoryRequestConfig;
import org.jboss.sbomer.core.config.request.ImageRequestConfig;
import org.jboss.sbomer.core.config.request.PncAnalysisRequestConfig;
import org.jboss.sbomer.core.config.request.PncBuildRequestConfig;
import org.jboss.sbomer.core.config.request.PncOperationRequestConfig;
import org.jboss.sbomer.core.config.request.RequestConfig;
import org.jboss.sbomer.core.dto.v1beta1.V1BaseBeta1RequestRecord;
import org.jboss.sbomer.core.dto.v1beta1.V1Beta1BaseGenerationRecord;
import org.jboss.sbomer.core.dto.v1beta1.V1Beta1BaseManifestRecord;
import org.jboss.sbomer.core.dto.v1beta1.V1Beta1RequestRecord;
import org.jboss.sbomer.core.errors.ClientException;
import org.jboss.sbomer.core.features.sbom.config.Config;
import org.jboss.sbomer.core.features.sbom.enums.GenerationRequestType;
import org.jboss.sbomer.core.features.sbom.enums.RequestEventType;
import org.jboss.sbomer.core.features.sbom.enums.UMBConsumer;
import org.jboss.sbomer.core.features.sbom.enums.UMBMessageStatus;
import org.jboss.sbomer.core.features.sbom.utils.SbomUtils;
import org.jboss.sbomer.service.feature.sbom.model.RequestEvent;
import org.jboss.sbomer.service.rest.QueryParameters;
import org.jboss.sbomer.service.rest.criteria.CriteriaAwareRepository;

import com.fasterxml.jackson.databind.JsonNode;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.Query;

@ApplicationScoped
public class RequestEventRepository extends CriteriaAwareRepository<RequestEvent> {

    public RequestEventRepository() {
        super(RequestEvent.class);
    }

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

    protected Instant convertFromTimestamp(Object rawTimeObject) {
        return (Instant) rawTimeObject;
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

    public List<V1BaseBeta1RequestRecord> searchRequestRecords(QueryParameters parameters) {
        return searchProjected(V1BaseBeta1RequestRecord.class, parameters, (query, builder, root) -> {
            return query.select(
                    builder.construct(
                            V1BaseBeta1RequestRecord.class,
                            root.get("id"),
                            root.get("receivalTime"),
                            root.get("eventType"),
                            root.get("requestConfig"),
                            root.get("event")));
        });
    }

    private static final Set<String> ALLOWED_TYPE_KEYS = Set.of(
            "id",
            ErrataAdvisoryRequestConfig.TYPE_NAME,
            ImageRequestConfig.TYPE_NAME,
            PncAnalysisRequestConfig.TYPE_NAME,
            PncBuildRequestConfig.TYPE_NAME,
            PncOperationRequestConfig.TYPE_NAME);

    private static final Map<String, Class<? extends RequestConfig>> TYPE_TO_CONFIG_CLASS = Map.of(
            ImageRequestConfig.TYPE_NAME,
            ImageRequestConfig.class,
            ErrataAdvisoryRequestConfig.TYPE_NAME,
            ErrataAdvisoryRequestConfig.class,
            PncAnalysisRequestConfig.TYPE_NAME,
            PncAnalysisRequestConfig.class,
            PncOperationRequestConfig.TYPE_NAME,
            PncOperationRequestConfig.class,
            PncBuildRequestConfig.TYPE_NAME,
            PncBuildRequestConfig.class);

    public List<V1Beta1RequestRecord> searchAggregatedResultsNatively(String filter) {
        if (filter == null || filter.isBlank()) {
            throw new ClientException("Filter cannot be null or empty.");
        }
        String[] parameters = filter.split("=");
        if (parameters.length != 2 || parameters[1] == null || parameters[1].isBlank()) {
            throw new ClientException("Invalid filter format. Expected 'key=value'.");
        }

        String typeKey = parameters[0].trim();
        String typeValue = parameters[1].trim();
        if (!ALLOWED_TYPE_KEYS.contains(typeKey)) {
            throw new ClientException("Unsupported typeKey '" + typeKey + "'. Allowed values: " + ALLOWED_TYPE_KEYS);
        }

        // Build the native SQL query
        StringBuilder sb = new StringBuilder().append("SELECT ")
                .append("re.id AS request_id, ")
                .append("re.receival_time AS receival_time, ")
                .append("re.event_type AS event_type, ")
                .append("re.request_config AS request_config, ")
                .append("re.event AS event, ")
                .append("s.id AS sbom_id, ")
                .append("s.identifier AS sbom_identifier, ")
                .append("s.root_purl AS sbom_root_purl, ")
                .append("s.creation_time AS sbom_creation_time, ")
                .append("s.config_index AS sbom_config_index, ")
                .append("s.status_msg AS sbom_status_msg, ")
                .append("sgr.id AS generation_request_id, ")
                .append("sgr.identifier AS generation_request_identifier, ")
                .append("sgr.config AS generation_request_config, ")
                .append("sgr.type AS generation_request_type, ")
                .append("sgr.creation_time AS generation_request_creation_time ")
                .append("FROM request re ")
                .append("LEFT JOIN sbom_generation_request sgr ON re.id = sgr.request_id ")
                .append("LEFT JOIN sbom s ON sgr.id = s.generationrequest_id ");

        Map<String, Object> params = filterAndBuildQueryParams(sb, typeKey, typeValue);
        Query query = getEntityManager().createNativeQuery(sb.toString());
        params.forEach(query::setParameter);

        // Execute the query and fetch results
        return aggregateResults(query.getResultList());
    }

    private Map<String, Object> filterAndBuildQueryParams(StringBuilder sb, String typeKey, String typeValue) {
        if ("id".equals(typeKey)) {
            sb.append("WHERE re.id = :id");
            return Map.of("id", typeValue);
        }

        String identifierKey = getValueOfField(TYPE_TO_CONFIG_CLASS.get(typeKey), "IDENTIFIER_KEY");
        addConfigCondition(sb, "WHERE", REQUEST_CONFIG_TYPE, "=");
        addConfigCondition(sb, "AND", identifierKey, "=");
        return Map.of(REQUEST_CONFIG_TYPE, typeKey, identifierKey, typeValue);
    }

    private List<V1Beta1RequestRecord> aggregateResults(List<Object[]> results) {

        // Aggregate results into a Map for grouping by RequestEvent
        Map<String, V1Beta1RequestRecord> aggregatedResults = new LinkedHashMap<>();

        results.forEach(row -> {
            String requestId = (String) row[0];
            Instant receivalTime = convertFromTimestamp(row[1]);
            RequestEventType eventType = RequestEventType.valueOf((String) row[2]);
            RequestConfig requestConfig = RequestConfig.fromString((String) row[3], RequestConfig.class);
            JsonNode event = SbomUtils.toJsonNode((String) row[4]);
            String sbomId = (String) row[5];

            if (sbomId != null) {
                V1Beta1BaseManifestRecord manifest = new V1Beta1BaseManifestRecord(
                        sbomId,
                        (String) row[6],
                        (String) row[7],
                        convertFromTimestamp(row[8]),
                        (Integer) row[9],
                        (String) row[10],
                        new V1Beta1BaseGenerationRecord(
                                (String) row[11],
                                (String) row[12],
                                Config.fromString((String) row[13]),
                                GenerationRequestType.valueOf((String) row[14]),
                                convertFromTimestamp(row[15])));

                aggregatedResults
                        .computeIfAbsent(
                                requestId,
                                id -> new V1Beta1RequestRecord(
                                        id,
                                        receivalTime,
                                        eventType,
                                        requestConfig,
                                        event,
                                        new ArrayList<>()))
                        .manifests()
                        .add(manifest);
            } else {
                aggregatedResults.computeIfAbsent(
                        requestId,
                        id -> new V1Beta1RequestRecord(
                                id,
                                receivalTime,
                                eventType,
                                requestConfig,
                                event,
                                new ArrayList<>()));
            }
        });

        return aggregatedResults.values()
                .stream()
                .sorted((record1, record2) -> record2.receivalTime().compareTo(record1.receivalTime()))
                .toList();
    }

}