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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.jboss.sbomer.core.features.sbom.enums.UMBConsumer;
import org.jboss.sbomer.core.features.sbom.enums.UMBMessageStatus;
import org.jboss.sbomer.core.features.sbom.enums.UMBMessageType;

import com.fasterxml.jackson.databind.JsonNode;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.transaction.Transactional;
import jakarta.persistence.Index;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@Entity
@ToString
@Table(
        name = "umb_message",
        indexes = { @Index(name = "idx_umb_message_type", columnList = "type"),
                @Index(name = "idx_umb_message_consumer", columnList = "consumer"),
                @Index(name = "idx_umb_message_status", columnList = "status") })
@NoArgsConstructor
@AllArgsConstructor
@Builder(setterPrefix = "with")
@RegisterForReflection
@Slf4j
public class UMBMessage extends PanacheEntityBase {

    @Id
    @Column(nullable = false, updatable = false)
    private String id;

    @Column(name = "consumer", nullable = false, updatable = false)
    @Enumerated(EnumType.STRING)
    private UMBConsumer consumer;

    @Column(name = "receival_time", nullable = false, updatable = false)
    private Instant receivalTime;

    @Column(name = "status", nullable = false, updatable = true)
    @Enumerated(EnumType.STRING)
    private UMBMessageStatus status;

    @Column(name = "type", nullable = true, updatable = true)
    @Enumerated(EnumType.STRING)
    private UMBMessageType type;

    @Column(name = "msg_id", nullable = true, updatable = true)
    private String msgId;

    @Column(name = "creation_time", nullable = true, updatable = true)
    private Instant creationTime;

    @Column(nullable = true, updatable = true)
    private String topic;

    @JdbcTypeCode(SqlTypes.JSON)
    @ToString.Exclude
    @Schema(implementation = Map.class) // Workaround for swagger limitation of not being able to digest through a very
    private JsonNode content;

    public static long countPncProcessedMessages() {
        return find(
                "consumer = ?1 and type <> ?2 and status = ?3",
                UMBConsumer.PNC,
                UMBMessageType.UNKNOWN,
                UMBMessageStatus.ACK).count();
    }

    public static long countErrataProcessedMessages() {
        return find(
                "consumer = ?1 and type <> ?2 and status = ?3",
                UMBConsumer.ERRATA,
                UMBMessageType.UNKNOWN,
                UMBMessageStatus.ACK).count();
    }

    public static long countPncSkippedMessages() {
        return find(
                "consumer = ?1 and type <> ?2 and status = ?3",
                UMBConsumer.PNC,
                UMBMessageType.UNKNOWN,
                UMBMessageStatus.SKIPPED).count();
    }

    public static long countErrataSkippedMessages() {
        return find(
                "consumer = ?1 and type <> ?2 and status = ?3",
                UMBConsumer.ERRATA,
                UMBMessageType.UNKNOWN,
                UMBMessageStatus.SKIPPED).count();
    }

    public static long countPncReceivedMessages() {
        return find("consumer = ?1", UMBConsumer.PNC).count();
    }

    public static long countErrataReceivedMessages() {
        return find("consumer = ?1", UMBConsumer.ERRATA).count();
    }

    public static long countAlreadyAckedWithMsgId(String msgId) {
        return find("msgId = ?1 AND status = ?2", msgId, UMBMessageStatus.ACK).count();
    }

    public static UMBMessage createNew(UMBConsumer consumer) {
        return UMBMessage.builder()
                .withId(RandomStringIdGenerator.generate())
                .withConsumer(consumer)
                .withReceivalTime(Instant.now())
                .withStatus(UMBMessageStatus.NONE)
                .build();
    }

    @Transactional
    public UMBMessage ackAndSave() {
        setStatus(UMBMessageStatus.ACK);
        persistAndFlush();
        return this;
    }

    @Transactional
    public UMBMessage skipAndSave() {
        setStatus(UMBMessageStatus.SKIPPED);
        persistAndFlush();
        return this;
    }

    @Transactional
    public UMBMessage nackAndSave() {
        setStatus(UMBMessageStatus.NACK);
        persistAndFlush();
        return this;
    }

}
