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
package org.jboss.sbomer.model;

import static org.jboss.sbomer.core.utils.SbomUtils.schemaVersion;

import java.time.Instant;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotBlank;

import org.cyclonedx.BomGeneratorFactory;
import org.cyclonedx.exception.ParseException;
import org.cyclonedx.generators.json.BomJsonGenerator;
import org.cyclonedx.model.Bom;
import org.cyclonedx.parsers.JsonParser;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.jboss.sbomer.core.enums.GeneratorImplementation;
import org.jboss.sbomer.core.enums.ProcessorImplementation;
import org.jboss.sbomer.utils.enums.SbomType;
import org.jboss.sbomer.validation.CycloneDxBom;

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
        indexes = { @Index(name = "idx_sbom_buildid", columnList = "build_id") },
        uniqueConstraints = @UniqueConstraint(
                name = "uq_sbom_buildid_generator_processor",
                columnNames = { "build_id", "generator", "processor" }))
@NamedQueries({ @NamedQuery(name = Sbom.FIND_ALL_BY_BUILDID, query = "FROM Sbom WHERE buildId = ?1"),
        @NamedQuery(
                name = Sbom.FIND_BASE_BY_BUILDID_GENERATOR,
                query = "FROM Sbom WHERE buildId = ?1 AND generator = ?2 and processor IS NULL"),
        @NamedQuery(
                name = Sbom.FIND_BY_BUILDID_GENERATOR_PROCESSOR,
                query = "FROM Sbom WHERE buildId = ?1 AND generator = ?2 AND processor = ?3") })
public class Sbom extends PanacheEntityBase {

    public static final String FIND_ALL_BY_BUILDID = "Sbom.findAllByBuildId";
    public static final String FIND_BASE_BY_BUILDID_GENERATOR = "Sbom.findBaseByBuildIdGenerator";
    public static final String FIND_BY_BUILDID_GENERATOR_PROCESSOR = "Sbom.findByBuildIdGeneratorProcessor";

    @Id
    @Column(nullable = false, updatable = false)
    @ToString.Exclude
    private Long id;

    @Column(name = "build_id", nullable = false, updatable = false)
    @NotBlank(message = "Build identifier missing")
    private String buildId;

    @Column(name = "generation_time", nullable = false, updatable = false)
    private Instant generationTime;

    @Column(name = "type", nullable = false, updatable = false)
    @Enumerated(EnumType.STRING)
    private SbomType type;

    @Column(name = "generator", nullable = false, updatable = false)
    @Enumerated(EnumType.STRING)
    private GeneratorImplementation generator;

    @Column(name = "processor", nullable = true, updatable = false)
    @Enumerated(EnumType.STRING)
    private ProcessorImplementation processor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "parent_sbom_id",
            foreignKey = @ForeignKey(name = "fk_sbom_parent_sbom"),
            nullable = true,
            updatable = false)
    @ToString.Exclude
    private Sbom parentSbom;

    @Type(type = JsonTypes.JSON_BIN)
    @Column(name = "sbom", columnDefinition = JsonTypes.JSON_BIN)
    @CycloneDxBom
    @ToString.Exclude
    private JsonNode sbom;

    @JsonIgnore
    public Bom getCycloneDxBom() {
        try {
            return new JsonParser().parse(sbom.isTextual() ? sbom.textValue().getBytes() : sbom.toString().getBytes());
        } catch (ParseException e) {
            e.printStackTrace();
        }

        try {
            BomJsonGenerator generator = BomGeneratorFactory.createJson(schemaVersion(), new Bom());
            return new JsonParser().parse(generator.toJsonNode().textValue().getBytes());
        } catch (ParseException e) {
            return null;
        }

    }
}
