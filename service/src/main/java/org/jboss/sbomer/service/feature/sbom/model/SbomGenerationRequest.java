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

import java.io.IOException;
import java.time.Instant;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.PrePersist;
import javax.persistence.Table;
import javax.transaction.Transactional;

import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.jboss.resteasy.spi.ApplicationException;
import org.jboss.sbomer.core.features.sbom.config.runtime.Config;
import org.jboss.sbomer.core.features.sbom.enums.GenerationResult;
import org.jboss.sbomer.core.features.sbom.utils.ObjectMapperProvider;
import org.jboss.sbomer.core.features.sbom.utils.SbomUtils;
import org.jboss.sbomer.service.feature.sbom.k8s.model.GenerationRequest;
import org.jboss.sbomer.service.feature.sbom.k8s.model.SbomGenerationStatus;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.JsonNode;

import io.quarkiverse.hibernate.types.json.JsonBinaryType;
import io.quarkiverse.hibernate.types.json.JsonTypes;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
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
        indexes = { @Index(name = "idx_request_buildid", columnList = "build_id"),
                @Index(name = "idx_request_status", columnList = "status") })
@Slf4j
@NoArgsConstructor
@AllArgsConstructor
@TypeDef(name = JsonTypes.JSON_BIN, typeClass = JsonBinaryType.class)
@Builder(setterPrefix = "with")
public class SbomGenerationRequest extends PanacheEntityBase {

    @Id
    @Column(nullable = false, updatable = false)
    private String id;

    @Column(name = "creation_time", nullable = false, updatable = false)
    private Instant creationTime;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    SbomGenerationStatus status;

    @Column(name = "result", nullable = true, updatable = true)
    @Enumerated(EnumType.STRING)
    GenerationResult result;

    @Column(name = "build_id", nullable = false, updatable = false)
    String buildId;

    @Type(type = JsonTypes.JSON_BIN)
    @Column(name = "config", columnDefinition = JsonTypes.JSON_BIN)
    @ToString.Exclude
    private JsonNode config;

    @Column(name = "reason", nullable = true, updatable = true)
    @Type(type = "org.hibernate.type.TextType")
    String reason;

    /**
     * Returns the config {@link Config}.
     *
     * In case the runtime config is not available or parsable, returns <code>null</code>.
     *
     * @return The {@link Config} object
     */
    @JsonIgnore
    public Config getConfiguration() {
        return SbomUtils.fromJsonConfig(config);
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
        SbomGenerationRequest sbomGenerationRequest = SbomGenerationRequest.findById(generationRequest.getId());

        // Create the entity if it's not there
        if (sbomGenerationRequest == null) {
            log.debug(
                    "Could not find SbomGenerationRequest entity in the database for id '{}', creating new one",
                    generationRequest.getId());

            sbomGenerationRequest = SbomGenerationRequest.builder()
                    .withId(generationRequest.getId())
                    .withBuildId(generationRequest.getBuildId())
                    .build();
        }

        // Finally sync the SbomGenerationRequest entity with the GenerationRequest.
        sbomGenerationRequest.setStatus(generationRequest.getStatus());
        // And reason
        sbomGenerationRequest.setReason(generationRequest.getReason());
        // And result
        sbomGenerationRequest.setResult(generationRequest.getResult());

        // Update config, if available
        if (generationRequest.getConfig() != null) {
            try {
                sbomGenerationRequest.setConfig(
                        SbomUtils.toJsonNode(
                                ObjectMapperProvider.yaml()
                                        .readValue(generationRequest.getConfig().getBytes(), Config.class)));
            } catch (IOException e) {
                throw new ApplicationException("Could not convert configuration to store in the database", e);
            }
        }

        // Store it in the database
        sbomGenerationRequest.persistAndFlush();

        log.debug(
                "SbomGenerationRequest '{}' synced with GenerationRequest '{}'",
                sbomGenerationRequest.getId(),
                generationRequest.getMetadata().getName());

        return sbomGenerationRequest;
    }

    @PrePersist
    public void prePersist() {
        creationTime = Instant.now();
    }

}
