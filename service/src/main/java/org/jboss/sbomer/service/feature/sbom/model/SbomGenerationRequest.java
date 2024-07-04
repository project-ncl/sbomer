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
import java.util.List;
import java.util.Map;

import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.jboss.sbomer.core.features.sbom.config.Config;
import org.jboss.sbomer.core.features.sbom.enums.GenerationRequestType;
import org.jboss.sbomer.core.features.sbom.enums.GenerationResult;
import org.jboss.sbomer.service.feature.sbom.k8s.model.GenerationRequest;
import org.jboss.sbomer.service.feature.sbom.k8s.model.SbomGenerationStatus;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
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
@Table(
        name = "sbom_generation_request",
        indexes = { @Index(name = "idx_request_identifier", columnList = "identifier"),
                @Index(name = "idx_request_type", columnList = "type"),
                @Index(name = "idx_request_status", columnList = "status") })
@Slf4j
@NoArgsConstructor
@AllArgsConstructor
@Builder(setterPrefix = "with")
@RegisterForReflection
public class SbomGenerationRequest extends PanacheEntityBase {

    @Id
    @Column(nullable = false, updatable = false)
    private String id;

    @Column(name = "creation_time", nullable = false, updatable = false)
    private Instant creationTime;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    SbomGenerationStatus status;

    @Column(name = "type", nullable = false)
    @Enumerated(EnumType.STRING)
    GenerationRequestType type;

    @Column(name = "result", nullable = true, updatable = true)
    @Enumerated(EnumType.STRING)
    GenerationResult result;

    @Column(name = "identifier", nullable = false, updatable = false)
    String identifier;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "config")
    @ToString.Exclude
    @Schema(implementation = Map.class)
    private Config config;

    @Column(name = "reason", nullable = true, updatable = true)
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    String reason;

    /**
     * Method to sync the {@link GenerationRequest} Kubernetes resource with the {@link SbomGenerationRequest} entity in
     * the database.
     *
     * @param generationRequest
     * @return Updated {@link SbomGenerationRequest} entity
     */
    @Transactional
    public static SbomGenerationRequest sync(GenerationRequest generationRequest) {
        SbomGenerationRequest sbomGenerationRequest = SbomGenerationRequest.findById(generationRequest.getId()); // NOSONAR

        // Create the entity if it's not there
        if (sbomGenerationRequest == null) {
            log.debug(
                    "Could not find SbomGenerationRequest entity in the database for id '{}', creating new one",
                    generationRequest.getId());

            sbomGenerationRequest = SbomGenerationRequest.builder()
                    .withId(generationRequest.getId())
                    .withIdentifier(generationRequest.getIdentifier())
                    .withType(generationRequest.getType())
                    .build();
        }

        // Finally sync the SbomGenerationRequest entity with the GenerationRequest.
        sbomGenerationRequest.setStatus(generationRequest.getStatus());
        // And reason
        sbomGenerationRequest.setReason(generationRequest.getReason());
        // And result
        sbomGenerationRequest.setResult(generationRequest.getResult());
        // And config
        sbomGenerationRequest.setConfig(generationRequest.getConfig());

        // Store it in the database
        sbomGenerationRequest.persistAndFlush();

        log.debug(
                "SbomGenerationRequest '{}' synced with GenerationRequest '{}'",
                sbomGenerationRequest.getId(),
                generationRequest.getMetadata().getName());

        return sbomGenerationRequest;
    }

    @Transactional
    public static List<SbomGenerationRequest> findPendingRequests(String operationId) {
        return SbomGenerationRequest.find( // NOSONAR
                "type = ?1 and identifier = ?2 and status = ?3 order by creationTime asc",
                GenerationRequestType.OPERATION,
                operationId,
                SbomGenerationStatus.NO_OP).list();
    }

    @PrePersist
    public void prePersist() {
        creationTime = Instant.now();
    }

}
