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
package org.jboss.sbomer.service.feature.sbom.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;
import javax.transaction.Transactional;

import org.hibernate.annotations.DynamicUpdate;
import org.jboss.sbomer.service.feature.sbom.k8s.model.GenerationRequest;
import org.jboss.sbomer.service.feature.sbom.k8s.model.SbomGenerationStatus;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@DynamicUpdate
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@Entity
@ToString
@Table(
        name = "sbom_generation_request",
        indexes = { @Index(name = "idx_request_buildid", columnList = "build_id"),
                @Index(name = "idx_request_status", columnList = "status") })
@Slf4j
public class SbomGenerationRequest extends PanacheEntityBase {

    @Id
    @Column(nullable = false, updatable = false)
    private String id;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    SbomGenerationStatus status;

    @Column(name = "build_id", nullable = false, updatable = false)
    String buildId;

    /**
     * Method to sync the {@link GenerationRequest} Kubernetes resource with the {@link SbomGenerationRequest} entity in
     * the database.
     *
     * @param generationRequest
     * @return Updated {@link SbomGenerationRequest} entity
     */
    @Transactional
    public static SbomGenerationRequest sync(GenerationRequest generationRequest) {
        SbomGenerationRequest sbomGenerationRequest = SbomGenerationRequest.findById(generationRequest.getId());

        // Create the entity if it's not there
        if (sbomGenerationRequest == null) {
            log.debug(
                    "Could not find SbomGenerationRequest entity in the database for id '{}', creating new one",
                    generationRequest.getId());

            sbomGenerationRequest = new SbomGenerationRequest();
            sbomGenerationRequest.setId(generationRequest.getId());
            sbomGenerationRequest.setBuildId(generationRequest.getBuildId());
        }

        // Finally sync the SbomGenerationRequest entity with the GenerationRequest.
        sbomGenerationRequest.setStatus(generationRequest.getStatus());

        // Store it in the database
        sbomGenerationRequest.persistAndFlush();

        log.debug(
                "SbomGenerationRequest '{}' synced with GenerationRequest '{}'",
                sbomGenerationRequest.getId(),
                generationRequest.getMetadata().getName());

        return sbomGenerationRequest;
    }

}
