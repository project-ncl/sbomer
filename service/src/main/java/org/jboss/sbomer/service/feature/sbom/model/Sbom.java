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

import static org.jboss.sbomer.core.features.sbom.utils.SbomUtils.schemaVersion;

import java.time.Instant;
import java.util.Map;

import org.cyclonedx.exception.ParseException;
import org.cyclonedx.generators.BomGeneratorFactory;
import org.cyclonedx.generators.json.BomJsonGenerator;
import org.cyclonedx.model.Bom;
import org.cyclonedx.parsers.JsonParser;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.jboss.sbomer.core.features.sbom.utils.SbomUtils;
import org.jboss.sbomer.core.features.sbom.validation.CycloneDxBom;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@DynamicUpdate
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@Entity
@ToString
@Table(
        name = "sbom",
        indexes = { @Index(name = "idx_sbom_identifier", columnList = "identifier"),
                @Index(name = "idx_sbom_rootpurl", columnList = "root_purl") })
@NoArgsConstructor
@AllArgsConstructor
@Builder(setterPrefix = "with")
@RegisterForReflection
public class Sbom extends PanacheEntityBase {

    @Id
    @Column(nullable = false, updatable = false)
    private String id;

    @Column(name = "identifier", nullable = false, updatable = false)
    @NotBlank(message = "Identifier missing")
    private String identifier;

    @Column(name = "root_purl")
    private String rootPurl;

    @Column(name = "creation_time", nullable = false, updatable = false)
    private Instant creationTime;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "sbom")
    @CycloneDxBom
    @ToString.Exclude
    @Schema(implementation = Map.class) // Workaround for swagger limitation of not being able to digest through a very
                                        // big schema which is the case if we would use the Bom.class
    private JsonNode sbom;

    @Column(name = "config_index")
    private Integer configIndex;

    @ManyToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinColumn(foreignKey = @ForeignKey(name = "fk_sbom_generationrequest"))
    private SbomGenerationRequest generationRequest;

    @JsonIgnore
    @Lob
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "status_msg")
    private String statusMessage;

    /**
     * Updates the purl for the object based on the SBOM content, if provided.
     *
     */
    private void setupRootPurl() {
        Bom bom = SbomUtils.fromJsonNode(getSbom());

        rootPurl = null;

        if (bom != null && bom.getMetadata() != null && bom.getMetadata().getComponent() != null) {
            rootPurl = bom.getMetadata().getComponent().getPurl();
        }
    }

    @PrePersist
    public void prePersist() {
        creationTime = Instant.now();
        setupRootPurl();
    }

    @PreUpdate
    public void preUpdate() {
        setupRootPurl();
    }

}
