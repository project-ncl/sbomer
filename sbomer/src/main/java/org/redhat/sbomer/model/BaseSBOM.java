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
package org.redhat.sbomer.model;

import java.time.Instant;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotBlank;

import org.cyclonedx.BomGeneratorFactory;
import org.cyclonedx.CycloneDxSchema.Version;
import org.cyclonedx.exception.ParseException;
import org.cyclonedx.generators.json.BomJsonGenerator;
import org.cyclonedx.model.Bom;
import org.cyclonedx.parsers.JsonParser;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.redhat.sbomer.validation.CycloneDxBom;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;

import io.quarkiverse.hibernate.types.json.JsonBinaryType;
import io.quarkiverse.hibernate.types.json.JsonTypes;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import static org.redhat.sbomer.utils.SbomUtils.schemaVersion;

@DynamicUpdate
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@Entity
@TypeDef(name = JsonTypes.JSON_BIN, typeClass = JsonBinaryType.class)
@ToString
@Table(
        name = "base_sbom",
        indexes = { @Index(name = "idx_basesbom_buildid", columnList = "build_id") },
        uniqueConstraints = @UniqueConstraint(name = "uq_basesbom_buildid", columnNames = { "build_id" }))
@NamedQueries({ @NamedQuery(name = BaseSBOM.FIND_BY_BUILDID, query = "FROM BaseSBOM WHERE buildId = ?1") })
public class BaseSBOM extends PanacheEntityBase {

    public static final String FIND_BY_BUILDID = "BaseSBOM.findByBuildId";

    @Id
    @Column(nullable = false, updatable = false)
    private Long id;

    @Column(name = "build_id", nullable = false, updatable = false)
    @NotBlank(message = "Build identifier missing")
    private String buildId;

    private Instant generationTime;

    @Type(type = JsonTypes.JSON_BIN)
    @Column(name = "sbom", columnDefinition = JsonTypes.JSON_BIN)
    @CycloneDxBom
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
