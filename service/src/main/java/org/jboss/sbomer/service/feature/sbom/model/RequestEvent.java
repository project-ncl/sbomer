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
import java.util.Objects;

import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.type.SqlTypes;
import org.jboss.sbomer.core.config.request.RequestConfig;
import org.jboss.sbomer.core.features.sbom.enums.RequestEventStatus;
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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@JsonInclude(Include.NON_NULL)
@DynamicUpdate
@Getter
@Setter
@Entity
@ToString
@Table(
        name = "request",
        indexes = { @Index(name = "idx_request_eventtype", columnList = "event_type"),
                @Index(name = "idx_request_eventstatus", columnList = "event_status") })
@Slf4j
@NoArgsConstructor
@AllArgsConstructor
@Builder(setterPrefix = "with")
@RegisterForReflection
public class RequestEvent extends PanacheEntityBase {

    public static final String IGNORED_UNKNOWN_REASON = "The message type is unknown";
    public static final String IGNORED_DUPLICATED_REASON = "The message was already acknowledged";
    public static final String FAILED_GENERIC_REASON = "An error occurred while processing";

    // The event keys for the UMB event type
    public static final String EVENT_KEY_UMB_CONSUMER = "consumer";
    public static final String EVENT_KEY_UMB_MSG_STATUS = "msg_status";
    public static final String EVENT_KEY_UMB_MSG = "msg";
    public static final String EVENT_KEY_UMB_MSG_TYPE = "msg_type";
    public static final String EVENT_KEY_UMB_MSG_CREATION_TIME = "creation_time";
    public static final String EVENT_KEY_UMB_MSG_ID = "msg_id";
    public static final String EVENT_KEY_UMB_TOPIC = "destination";

    public static final String EVENT_VALUE_UMB_UNKNOWN_MSG_TYPE = "unknown";

    // The event keys for REST event type
    public static final String EVENT_KEY_REST_METHOD = "method";
    public static final String EVENT_KEY_REST_ADDRESS = "address";
    public static final String EVENT_KEY_REST_USERNAME = "username";
    public static final String EVENT_KEY_REST_TRACE_ID = "trace_id";
    public static final String EVENT_KEY_REST_SPAN_ID = "span_id";
    public static final String EVENT_KEY_REST_URI_PATH = "destination";

    // The type of request event (UMB | REST)
    public static final String REQUEST_EVENT_TYPE = "event_type";

    // The request config type (type of concrete classes of org.jboss.sbomer.core.config.request.RequestConfig)
    public static final String REQUEST_CONFIG_TYPE = "type";

    @Id
    @Column(nullable = false, updatable = false)
    private String id;

    @Column(name = "receival_time", nullable = false, updatable = false)
    private Instant receivalTime;

    @Column(name = "event_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private RequestEventType eventType;

    @Column(name = "event_status", nullable = false)
    @Enumerated(EnumType.STRING)
    private RequestEventStatus eventStatus;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "request_config")
    @ToString.Exclude
    @Schema(implementation = Map.class)
    private RequestConfig requestConfig;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "event", nullable = false)
    @ToString.Exclude
    @Schema(implementation = Map.class)
    private JsonNode event;

    @Column(name = "reason")
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    String reason;

    public static RequestEvent createNew(RequestConfig requestConfig, RequestEventType eventType, Object event) {
        JsonNode eventNode = (event instanceof Map) ? ObjectMapperProvider.json().valueToTree(event) : (JsonNode) event;

        return RequestEvent.builder()
                .withId(RandomStringIdGenerator.generate())
                .withRequestConfig(requestConfig)
                .withReceivalTime(Instant.now())
                .withEventType(eventType)
                .withEventStatus(RequestEventStatus.IN_PROGRESS)
                .withEvent(eventNode)
                .build();
    }

    @Transactional
    public RequestEvent save() {
        persistAndFlush();
        return this;
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null) {
            return false;
        }

        Class<?> oEffectiveClass = (o instanceof HibernateProxy proxy)
                ? proxy.getHibernateLazyInitializer().getPersistentClass()
                : o.getClass();
        Class<?> thisEffectiveClass = (this instanceof HibernateProxy proxy)
                ? proxy.getHibernateLazyInitializer().getPersistentClass()
                : this.getClass();

        if (thisEffectiveClass != oEffectiveClass) {
            return false;
        }

        RequestEvent requestEvent = (RequestEvent) o;
        return Objects.equals(id, requestEvent.id);
    }

    @Override
    public final int hashCode() {
        return (this instanceof HibernateProxy proxy)
                ? proxy.getHibernateLazyInitializer().getPersistentClass().hashCode()
                : getClass().hashCode();
    }
}
