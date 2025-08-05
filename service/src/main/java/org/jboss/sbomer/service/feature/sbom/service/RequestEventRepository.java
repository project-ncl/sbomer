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
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
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
import org.jboss.sbomer.core.dto.v1beta1.V1Beta1GenerationRecord;
import org.jboss.sbomer.core.dto.v1beta1.V1Beta1RequestManifestRecord;
import org.jboss.sbomer.core.dto.v1beta1.V1Beta1RequestRecord;
import org.jboss.sbomer.core.errors.ClientException;
import org.jboss.sbomer.core.features.sbom.config.Config;
import org.jboss.sbomer.core.features.sbom.enums.RequestEventStatus;
import org.jboss.sbomer.core.features.sbom.enums.RequestEventType;
import org.jboss.sbomer.core.features.sbom.enums.UMBConsumer;
import org.jboss.sbomer.core.features.sbom.enums.UMBMessageStatus;
import org.jboss.sbomer.core.features.sbom.utils.SbomUtils;
import org.jboss.sbomer.service.feature.sbom.model.RequestEvent;
import org.jboss.sbomer.service.rest.QueryParameters;
import org.jboss.sbomer.service.rest.criteria.CriteriaAwareRepository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.Query;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

@ApplicationScoped
@Slf4j
public class RequestEventRepository extends CriteriaAwareRepository<RequestEvent> {
    private static final String BASE_COUNT_QUERY = "SELECT COUNT(*) FROM request";

    private static final String BASE_SELECT_QUERY = "SELECT * FROM request";

    private static final String WHERE = "WHERE";

    private static final String AND = "AND";

    private static final String EQUAL = "=";

    private static final String NOT_EQUAL = "<>";

    private static final String FIND_GENERATION_REQUEST_MANIFESTS_NATIVE_QUERY = "SELECT re.id AS request_id, "
            + "re.receival_time AS receival_time, re.event_type AS event_type, "
            + "re.event_status AS event_status, re.reason AS reason, re.request_config AS request_config, "
            + "re.event AS event, s.id AS sbom_id, s.identifier AS sbom_identifier, "
            + "s.root_purl AS sbom_root_purl, s.creation_time AS sbom_creation_time, "
            + "s.config_index AS sbom_config_index, s.status_msg AS sbom_status_msg, "
            + "sgr.id AS generation_request_id, sgr.identifier AS generation_request_identifier, "
            + "sgr.config AS generation_request_config, sgr.type AS generation_request_type, "
            + "sgr.creation_time AS generation_request_creation_time, sgr.status AS generation_request_status, "
            + "sgr.result AS generation_request_result, sgr.reason AS generation_request_reason "
            + "FROM request re LEFT JOIN sbom_generation_request sgr ON re.id = sgr.request_id "
            + "LEFT JOIN sbom s ON sgr.id = s.generationrequest_id ";

    private static final String FIND_MINIMIZED_GENERATION_REQUEST_MANIFESTS_NATIVE_QUERY = "SELECT re.id AS request_id, "
            + "re.receival_time AS receival_time, re.event_status AS event_status, "
            + "s.id AS sbom_id, s.identifier AS sbom_identifier, s.root_purl AS sbom_root_purl, "
            + "sgr.id AS generation_request_id, sgr.identifier AS generation_request_identifier, "
            + "sgr.config AS generation_request_config, sgr.type AS generation_request_type, sgr.status AS generation_request_status "
            + "FROM request re LEFT JOIN sbom_generation_request sgr ON re.id = sgr.request_id "
            + "LEFT JOIN sbom s ON sgr.id = s.generationrequest_id ";

    public RequestEventRepository() {
        super(RequestEvent.class);
    }

    @Transactional
    public RequestEvent updateWithFailure(String requestEventId, String reason) {
        RequestEvent requestEvent = RequestEvent.findById(requestEventId); // NOSONAR
        requestEvent.setEventStatus(RequestEventStatus.FAILED);
        requestEvent.setReason(reason);
        return requestEvent.save();
    }

    public long countUMBEventsWithStatusFrom(UMBMessageStatus status, UMBConsumer consumer) {
        StringBuilder query = initCountRequestQuery();
        addCondition(query, WHERE, REQUEST_EVENT_TYPE, EQUAL);
        addEventCondition(query, AND, EVENT_KEY_UMB_CONSUMER, EQUAL);
        addEventCondition(query, AND, EVENT_KEY_UMB_MSG_STATUS, EQUAL);
        addEventCondition(query, AND, EVENT_KEY_UMB_MSG_TYPE, NOT_EQUAL);

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
        addCondition(query, WHERE, REQUEST_EVENT_TYPE, EQUAL);
        addEventCondition(query, AND, EVENT_KEY_UMB_CONSUMER, EQUAL);

        Map<String, Object> params = Map.of(REQUEST_EVENT_TYPE, UMB.name(), EVENT_KEY_UMB_CONSUMER, consumer.name());

        return executeCountQuery(query.toString(), params);
    }

    public long countAlreadyAckedUMBEventsFor(String msgId) {
        StringBuilder query = initCountRequestQuery();
        addCondition(query, WHERE, REQUEST_EVENT_TYPE, EQUAL);
        addEventCondition(query, AND, EVENT_KEY_UMB_MSG_STATUS, EQUAL);
        addEventCondition(query, AND, EVENT_KEY_UMB_MSG_ID, EQUAL);

        Map<String, Object> params = Map
                .of(REQUEST_EVENT_TYPE, UMB.name(), EVENT_KEY_UMB_MSG_STATUS, ACK.name(), EVENT_KEY_UMB_MSG_ID, msgId);

        return executeCountQuery(query.toString(), params);
    }

    public long countEventsForTypeAndIdentifier(String typeValue, String identifierKey, String identifierValue) {
        StringBuilder query = initCountRequestQuery();
        addConfigCondition(query, WHERE, REQUEST_CONFIG_TYPE, EQUAL);
        addConfigCondition(query, AND, identifierKey, EQUAL);

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

    public RequestEvent updateRequestConfig(RequestEvent requestEvent, RequestConfig config) {
        requestEvent = RequestEvent.findById(requestEvent.getId()); // NOSONAR
        requestEvent.setRequestConfig(config);
        return requestEvent.save();
    }

    public RequestEvent updateRequestEvent(
            RequestEvent requestEvent,
            RequestEventStatus status,
            Map<String, String> extra,
            String reason) {
        requestEvent = RequestEvent.findById(requestEvent.getId()); // NOSONAR
        extra.forEach(((ObjectNode) requestEvent.getEvent())::put);

        if (status != null) {
            requestEvent.setEventStatus(status);
        }
        if (reason != null) {
            requestEvent.setReason(reason);
        }
        return requestEvent.save();
    }

    public RequestEvent createRequestEvent(RequestEventStatus status, ObjectNode event, String reason) {
        RequestEvent requestEvent = RequestEvent.createNew(null, RequestEventType.UMB, event);
        if (status != null) {
            requestEvent.setEventStatus(status);
        }
        if (reason != null) {
            requestEvent.setReason(reason);
        }
        return requestEvent.save();
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

    protected StringBuilder addReleaseMetadataCondition(
            StringBuilder query,
            String condition,
            String sbomAlias,
            String property,
            String operator) {
        query.append(" ")
                .append(condition)
                .append(sbomAlias != null && !sbomAlias.isEmpty() ? " " + sbomAlias + "." : " ")
                .append("release_metadata ->> '")
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
        return rawTimeObject instanceof Timestamp timestamp ? timestamp.toInstant() : (Instant) rawTimeObject;
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
        return searchProjected(
                V1BaseBeta1RequestRecord.class,
                parameters,
                (query, builder, root) -> query.select(
                        builder.construct(
                                V1BaseBeta1RequestRecord.class,
                                root.get("id"),
                                root.get("receivalTime"),
                                root.get("eventType"),
                                root.get("eventStatus"),
                                root.get("reason"),
                                root.get("requestConfig"),
                                root.get("event"))));
    }

    private static final String RELEASE_METADATA_ERRATA_ID = "release.errata_id";
    private static final String RELEASE_METADATA_ERRATA_FULLNAME = "release.errata_fullname";

    private static final Set<String> ALLOWED_TYPE_KEYS = Set.of(
            "id",
            ErrataAdvisoryRequestConfig.TYPE_NAME,
            ImageRequestConfig.TYPE_NAME,
            PncAnalysisRequestConfig.TYPE_NAME,
            PncBuildRequestConfig.TYPE_NAME,
            PncOperationRequestConfig.TYPE_NAME,
            RELEASE_METADATA_ERRATA_ID,
            RELEASE_METADATA_ERRATA_FULLNAME);

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

    public List<V1Beta1RequestRecord> searchAggregatedResultsNatively(String filter, boolean minimized) {
        if (filter == null || filter.isBlank()) {
            throw new ClientException("Filter cannot be null or empty.");
        }
        String[] parameters = filter.split(EQUAL);
        if (parameters.length != 2 || parameters[1] == null || parameters[1].isBlank()) {
            throw new ClientException("Invalid filter format. Expected 'key=value'.");
        }

        String typeKey = parameters[0].trim();
        String typeValue = parameters[1].trim();
        if (!ALLOWED_TYPE_KEYS.contains(typeKey)) {
            throw new ClientException("Unsupported typeKey '" + typeKey + "'. Allowed values: " + ALLOWED_TYPE_KEYS);
        }

        log.debug("Natively searching records for type '{}' and value '{}'", typeKey, typeValue);

        // Build the native SQL query
        StringBuilder sb = new StringBuilder().append(
                minimized ? FIND_MINIMIZED_GENERATION_REQUEST_MANIFESTS_NATIVE_QUERY
                        : FIND_GENERATION_REQUEST_MANIFESTS_NATIVE_QUERY);

        Map<String, Object> params = filterAndBuildQueryParams(sb, typeKey, typeValue);
        Query query = getEntityManager().createNativeQuery(sb.toString());
        params.forEach(query::setParameter);

        // Execute the query and fetch results
        return aggregateResults(query.getResultList(), minimized);
    }

    private Map<String, Object> filterAndBuildQueryParams(StringBuilder sb, String typeKey, String typeValue) {
        if ("id".equals(typeKey)) {
            sb.append("WHERE re.id = :id");
            return Map.of("id", typeValue);
        }
        if (RELEASE_METADATA_ERRATA_ID.equals(typeKey)) {
            sb.append("WHERE s.release_metadata is not null");
            addReleaseMetadataCondition(sb, AND, "s", "errata_id", EQUAL);
            return Map.of("errata_id", typeValue);
        }
        if (RELEASE_METADATA_ERRATA_FULLNAME.equals(typeKey)) {
            sb.append("WHERE s.release_metadata is not null");
            addReleaseMetadataCondition(sb, AND, "s", "errata_fullname", EQUAL);
            return Map.of("errata_fullname", typeValue);
        }

        String identifierKey = getValueOfField(TYPE_TO_CONFIG_CLASS.get(typeKey), "IDENTIFIER_KEY");
        addConfigCondition(sb, WHERE, REQUEST_CONFIG_TYPE, EQUAL);
        addConfigCondition(sb, AND, identifierKey, EQUAL);
        return Map.of(REQUEST_CONFIG_TYPE, typeKey, identifierKey, typeValue);
    }

    private List<V1Beta1RequestRecord> aggregateResults(List<Object[]> results, boolean minimized) {

        // Aggregate results into a Map for grouping by RequestEvent
        Map<String, V1Beta1RequestRecord> aggregatedResults = new LinkedHashMap<>();

        if (minimized) {
            results.forEach(row -> {
                convertMinimizedRow(row, aggregatedResults);
            });
        } else {
            results.forEach(row -> {
                convertRow(row, aggregatedResults);
            });
        }

        return aggregatedResults.values()
                .stream()
                .sorted(Comparator.comparing(V1Beta1RequestRecord::receivalTime).reversed())
                .toList();
    }

    private void convertRow(Object[] row, Map<String, V1Beta1RequestRecord> aggregatedResults) {
        String sbomId = (String) row[7];

        V1Beta1RequestRecord requestRecord = aggregatedResults.computeIfAbsent(
                (String) row[0],
                id -> new V1Beta1RequestRecord(
                        id,
                        convertFromTimestamp(row[1]),
                        RequestEventType.valueOf((String) row[2]),
                        RequestEventStatus.valueOf((String) row[3]),
                        (String) row[4],
                        RequestConfig.fromString((String) row[5], RequestConfig.class),
                        SbomUtils.toJsonNode((String) row[6]),
                        new ArrayList<>()));

        if (sbomId != null) {
            V1Beta1RequestManifestRecord manifest = new V1Beta1RequestManifestRecord(
                    sbomId,
                    (String) row[8],
                    (String) row[9],
                    convertFromTimestamp(row[10]),
                    (Integer) row[11],
                    (String) row[12],
                    new V1Beta1GenerationRecord(
                            (String) row[13],
                            (String) row[14],
                            Config.fromString((String) row[15]),
                            (String) row[16],
                            convertFromTimestamp(row[17]),
                            (String) row[18],
                            (String) row[19],
                            (String) row[20]));
            requestRecord.manifests().add(manifest);
        }
    }

    private void convertMinimizedRow(Object[] row, Map<String, V1Beta1RequestRecord> aggregatedResults) {
        String sbomId = (String) row[3];

        V1Beta1RequestRecord requestRecord = aggregatedResults.computeIfAbsent(
                (String) row[0],
                id -> new V1Beta1RequestRecord(
                        id,
                        convertFromTimestamp(row[1]),
                        null,
                        RequestEventStatus.valueOf((String) row[2]),
                        null,
                        null,
                        null,
                        new ArrayList<>()));

        if (sbomId != null) {
            V1Beta1RequestManifestRecord manifest = new V1Beta1RequestManifestRecord(
                    sbomId,
                    (String) row[4],
                    (String) row[5],
                    null,
                    null,
                    null,
                    new V1Beta1GenerationRecord(
                            (String) row[6],
                            (String) row[7],
                            Config.fromString((String) row[8]),
                            (String) row[9],
                            null,
                            (String) row[10],
                            null,
                            null));
            requestRecord.manifests().add(manifest);
        }
    }
}
