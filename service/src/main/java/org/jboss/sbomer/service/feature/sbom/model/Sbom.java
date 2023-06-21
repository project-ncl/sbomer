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

import static org.jboss.sbomer.core.features.sbom.utils.SbomUtils.schemaVersion;

import java.time.Instant;
import java.util.Set;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import org.cyclonedx.BomGeneratorFactory;
import org.cyclonedx.exception.ParseException;
import org.cyclonedx.generators.json.BomJsonGenerator;
import org.cyclonedx.model.Bom;
import org.cyclonedx.parsers.JsonParser;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.jboss.sbomer.core.features.sbom.enums.GeneratorType;
import org.jboss.sbomer.core.features.sbom.enums.ProcessorType;
import org.jboss.sbomer.core.features.sbom.enums.SbomStatus;
import org.jboss.sbomer.core.features.sbom.enums.SbomType;
import org.jboss.sbomer.core.features.sbom.utils.SbomUtils;
import org.jboss.sbomer.core.features.sbom.validation.CycloneDxBom;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;

import io.quarkiverse.hibernate.types.json.JsonBinaryType;
import io.quarkiverse.hibernate.types.json.JsonTypes;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@DynamicUpdate
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@Entity
@TypeDef(name = JsonTypes.JSON_BIN, typeClass = JsonBinaryType.class)
@ToString
@Table(
        name = "sbom",
        indexes = { @Index(name = "idx_sbom_buildid", columnList = "build_id"),
                @Index(name = "idx_sbom_rootpurl", columnList = "root_purl"),
                @Index(name = "idx_sbom_generator", columnList = "generator"),
                @Index(name = "idx_sbom_type", columnList = "type"),
                @Index(name = "idx_sbom_status", columnList = "status") })
public class Sbom extends PanacheEntityBase {

    @Id
    @Column(nullable = false, updatable = false)
    private Long id;

    @Column(name = "build_id", nullable = false, updatable = false)
    @NotBlank(message = "Build identifier missing")
    private String buildId;

    @Column(name = "root_purl")
    private String rootPurl;

    // TODO: rename this field? Is it covering the creation of the entity, started generation process time or finished
    // generation time?
    @Column(name = "generation_time", nullable = false, updatable = false)
    private Instant generationTime;

    @Column(name = "type", nullable = false, updatable = false)
    @Enumerated(EnumType.STRING)
    @NotNull(message = "Type not specified")
    private SbomType type; // TODO: dump

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private SbomStatus status = SbomStatus.NEW; // TODO: dump

    @Column(name = "generator", nullable = false, updatable = false)
    @Enumerated(EnumType.STRING)
    @NotNull(message = "Generator is required")
    private GeneratorType generator; // TODO: dump?

    @Column(name = "processors", nullable = false, updatable = false)
    @ElementCollection(targetClass = ProcessorType.class, fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    @CollectionTable(
            name = "sbom_processors",
            joinColumns = @JoinColumn(name = "sbom_id", foreignKey = @ForeignKey(name = "fk_processor_sbom")))
    private Set<ProcessorType> processors; // TODO: dump?

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(
            name = "parent_sbom_id",
            foreignKey = @ForeignKey(name = "fk_sbom_parent_sbom"),
            nullable = true,
            updatable = false)
    @ToString.Exclude
    private Sbom parentSbom; // TODO: dump

    @Type(type = JsonTypes.JSON_BIN)
    @Column(name = "sbom", columnDefinition = JsonTypes.JSON_BIN)
    @CycloneDxBom
    @ToString.Exclude
    private JsonNode sbom;

    @JsonIgnore
    @Lob
    @Type(type = "org.hibernate.type.TextType")
    @Column(name = "status_msg")
    private String statusMessage;

    /**
     * Creates a child of the current {@link Sbom} resource with status set to {@link SbomStatus#NEW}.
     *
     * @return New {@link Sbom} resource which parent is set to the current one.
     */
    public Sbom giveBirth() { // TODO: dump
        Sbom child = new Sbom();
        child.setBuildId(this.getBuildId());
        child.setGenerator(this.getGenerator());
        child.setProcessors(this.getProcessors());
        child.setType(this.getType());
        child.setParentSbom(this);

        return child;
    }

    /**
     * Returns the generated SBOM as the CycloneDX {@link Bom} object.
     *
     * In case the SBOM is not available yet, returns <code>null</code>.
     *
     * @return The {@link Bom} object representing the SBOM.
     */
    @JsonIgnore
    public Bom getCycloneDxBom() {
        if (sbom == null) {
            return null;
        }

        Bom bom = SbomUtils.fromJsonNode(sbom);

        if (bom == null) {
            try {
                BomJsonGenerator generator = BomGeneratorFactory.createJson(schemaVersion(), new Bom());
                bom = new JsonParser().parse(generator.toJsonNode().textValue().getBytes());
            } catch (ParseException e) {
            }
        }

        return bom;

    }

    /**
     * Updates the purl for the object based on the SBOM content, if provided.
     *
     */
    @JsonIgnore
    @PrePersist
    @PreUpdate
    private void setupRootPurl() {
        Bom bom = getCycloneDxBom();

        rootPurl = null;

        if (bom != null && bom.getMetadata() != null && bom.getMetadata().getComponent() != null) {
            rootPurl = bom.getMetadata().getComponent().getPurl();
        }
    }

    @JsonIgnore
    public boolean isBase() { // TODO: dump
        if (parentSbom == null) {
            return true;
        }

        return false;
    }
}
