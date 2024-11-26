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
package org.jboss.sbomer.service.feature.sbom.model;

import java.time.Instant;
import java.util.Map;

import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.jboss.sbomer.core.config.request.RequestConfig;
import org.jboss.sbomer.core.features.sbom.enums.RequestEventType;
import org.jboss.sbomer.core.features.sbom.utils.ObjectMapperProvider;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.JsonNode;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@JsonInclude(Include.NON_NULL)
@DynamicUpdate
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@ToString
@Table(name = "request", indexes = { @Index(name = "idx_request_eventtype", columnList = "event_type") })
@Slf4j
@NoArgsConstructor
@AllArgsConstructor
@Builder(setterPrefix = "with")
@RegisterForReflection
public class RequestEvent extends PanacheEntityBase {

    // The event keys for UMB event type
    public final static String EVENT_KEY_UMB_CONSUMER = "consumer";
    public final static String EVENT_KEY_UMB_MSG_STATUS = "msg_status";
    public final static String EVENT_KEY_UMB_MSG = "msg";
    public final static String EVENT_KEY_UMB_MSG_TYPE = "msg_type";
    public final static String EVENT_KEY_UMB_MSG_CREATION_TIME = "creation_time";
    public final static String EVENT_KEY_UMB_MSG_ID = "msg_id";
    public final static String EVENT_KEY_UMB_TOPIC = "destination";

    public final static String EVENT_VALUE_UMB_UNKNOWN_MSG_TYPE = "unknown";

    // The event keys for REST event type
    public final static String EVENT_KEY_REST_METHOD = "method";
    public final static String EVENT_KEY_REST_ADDRESS = "address";
    public final static String EVENT_KEY_REST_USERNAME = "username";
    public final static String EVENT_KEY_REST_TRACE_ID = "trace_id";
    public final static String EVENT_KEY_REST_SPAN_ID = "span_id";
    public final static String EVENT_KEY_REST_URI_PATH = "destination";

    // The type of request event (UMB | REST)
    public final static String REQUEST_EVENT_TYPE = "event_type";

    // The request config type (type of concrete classes of org.jboss.sbomer.core.config.request.RequestConfig)
    public final static String REQUEST_CONFIG_TYPE = "type";

    @Id
    @Column(nullable = false, updatable = false)
    private String id;

    @Column(name = "receival_time", nullable = false, updatable = false)
    private Instant receivalTime;

    @Column(name = "event_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private RequestEventType eventType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "request_config", nullable = true)
    @ToString.Exclude
    @Schema(implementation = Map.class)
    private RequestConfig requestConfig;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "event", nullable = false)
    @ToString.Exclude
    @Schema(implementation = Map.class)
    private JsonNode event;

    public static RequestEvent createNew(RequestConfig requestConfig, RequestEventType eventType, Object event) {
        JsonNode eventNode = (event instanceof Map) ? ObjectMapperProvider.json().valueToTree(event) : (JsonNode) event;

        return RequestEvent.builder()
                .withId(RandomStringIdGenerator.generate())
                .withRequestConfig(requestConfig)
                .withReceivalTime(Instant.now())
                .withEventType(eventType)
                .withEvent(eventNode)
                .build();
    }

    @Transactional
    public RequestEvent save() {
        persistAndFlush();
        return this;
    }

}
