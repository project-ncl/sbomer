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

import static org.jboss.sbomer.service.feature.sbom.errata.event.EventNotificationFiringUtil.notifyRequestEventStatusUpdate;

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
import org.jboss.sbomer.core.features.sbom.enums.RequestEventStatus;
import org.jboss.sbomer.service.feature.sbom.errata.event.comment.RequestEventStatusUpdateEvent;
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
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Query;
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

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(foreignKey = @ForeignKey(name = "fk_generationrequest_request"))
    private RequestEvent request;

    /**
     * Method to sync the {@link GenerationRequest} Kubernetes resource with the {@link SbomGenerationRequest} entity in
     * the database, with a provided request event.
     *
     * @param generationRequest
     * @param request
     * @return Updated {@link SbomGenerationRequest} entity
     */
    @Transactional
    public static SbomGenerationRequest sync(RequestEvent request, GenerationRequest generationRequest) {

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

        // If the request is null (e.g. sync called from the controllers) do not override it
        if (request != null) {
            RequestEvent dbRequestEvent = RequestEvent.findById(request.getId()); // NOSONAR
            if (dbRequestEvent == null) {
                dbRequestEvent = request.save();
            }
            sbomGenerationRequest.setRequest(dbRequestEvent);
        }

        // Update the status of the request of this generation
        updateRequestEventStatus(sbomGenerationRequest);

        // Store it in the database
        sbomGenerationRequest.persistAndFlush();

        log.debug(
                "SbomGenerationRequest '{}' synced with GenerationRequest '{}'",
                sbomGenerationRequest.getId(),
                generationRequest.getMetadata().getName());

        return sbomGenerationRequest;
    }

    @Transactional
    public static void updateRequestEventStatus(SbomGenerationRequest sbomGenerationRequest) {

        if (sbomGenerationRequest.getRequest() == null) {
            return;
        }

        Object[] results = countSbomGenerationRequestsOf(sbomGenerationRequest.getRequest());
        long generationsInProgress = results[0] == null ? 0L : ((Number) results[0]).longValue();
        long generationsFailed = results[1] == null ? 0L : ((Number) results[1]).longValue();
        long generationsTotal = results[2] == null ? 0L : ((Number) results[2]).longValue();

        // If this is not a final status update, mark the request as in progress
        if (!sbomGenerationRequest.getStatus().isFinal()) {
            sbomGenerationRequest.getRequest().setEventStatus(RequestEventStatus.IN_PROGRESS);
            if (generationsInProgress == 0) {
                // At least this one is in progress (it might have not been stored in DB yet)
                generationsInProgress = 1L;
            }
            sbomGenerationRequest.getRequest()
                    .setReason(generationsInProgress + "/" + generationsTotal + " in progress");
            return;
        }

        // This is a final status (FAILED or FINISHED)
        if (generationsInProgress > 0) {
            // There are still generations in progress for this request
            sbomGenerationRequest.getRequest().setEventStatus(RequestEventStatus.IN_PROGRESS);
            sbomGenerationRequest.getRequest()
                    .setReason(generationsInProgress + "/" + generationsTotal + " in progress");
            return;
        }

        if (generationsFailed > 0 || SbomGenerationStatus.FAILED.equals(sbomGenerationRequest.getStatus())) {
            // There are no more generations in progress and some failed
            long failed = generationsFailed
                    + (SbomGenerationStatus.FAILED.equals(sbomGenerationRequest.getStatus()) ? 1L : 0L);
            failed = Math.min(failed, generationsTotal);
            sbomGenerationRequest.getRequest().setReason(failed + "/" + generationsTotal + " failed");
            sbomGenerationRequest.getRequest().setEventStatus(RequestEventStatus.FAILED);

        } else {
            // There are no generations in progress nor failed
            sbomGenerationRequest.getRequest()
                    .setReason(generationsTotal + "/" + generationsTotal + " completed with success");
            sbomGenerationRequest.getRequest().setEventStatus(RequestEventStatus.SUCCESS);

        }

        // Send an async notification for the completed generations (will be used to add comments to Errata)
        notifyRequestEventStatusUpdate(
                RequestEventStatusUpdateEvent.builder()
                        .withRequestEventId(sbomGenerationRequest.getRequest().getId())
                        .withRequestEventConfig(sbomGenerationRequest.getRequest().getRequestConfig())
                        .withRequestEventStatus(sbomGenerationRequest.getRequest().getEventStatus())
                        .build());
    }

    @Transactional
    private static Object[] countSbomGenerationRequestsOf(RequestEvent request) {

        Query query = getEntityManager()
                .createQuery(
                        "SELECT SUM(CASE WHEN status != ?1 AND status != ?2 THEN 1 ELSE 0 END), "
                                + "SUM(CASE WHEN status = ?3 THEN 1 ELSE 0 END), " + "COUNT(*) AS total_count "
                                + "FROM SbomGenerationRequest WHERE request.id = ?4")
                .setParameter(1, SbomGenerationStatus.FAILED)
                .setParameter(2, SbomGenerationStatus.FINISHED)
                .setParameter(3, SbomGenerationStatus.FAILED)
                .setParameter(4, request.getId());

        return (Object[]) query.getSingleResult();
    }

    /**
     * Method to sync the {@link GenerationRequest} Kubernetes resource with the {@link SbomGenerationRequest} entity in
     * the database.
     *
     * @param generationRequest
     * @return Updated {@link SbomGenerationRequest} entity
     */
    @Transactional
    public static SbomGenerationRequest sync(GenerationRequest generationRequest) {
        return sync(null, generationRequest);
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
