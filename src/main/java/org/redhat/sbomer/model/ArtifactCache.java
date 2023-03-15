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

import org.cyclonedx.exception.ParseException;
import org.cyclonedx.model.Bom;
import org.cyclonedx.parsers.JsonParser;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.redhat.sbomer.validation.ArtifactJsonProperty;
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

@DynamicUpdate
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@Entity
@TypeDef(name = JsonTypes.JSON_BIN, typeClass = JsonBinaryType.class)
@ToString
@Table(
        name = "artifact_cache",
        indexes = { @Index(name = "idx_artifact_cache_purl", columnList = "purl") },
        uniqueConstraints = @UniqueConstraint(name = "uq_artifact_cache_purl", columnNames = { "purl" }))
@NamedQueries({ @NamedQuery(name = ArtifactCache.FIND_BY_PURL, query = "FROM ArtifactCache WHERE purl = ?1") })
public class ArtifactCache extends PanacheEntityBase {

    public static final String FIND_BY_PURL = "ArtifactCache.findByPurl";

    @Id
    @Column(nullable = false, updatable = false)
    private Long id;

    @Column(name = "purl", nullable = false, updatable = false)
    @NotBlank(message = "Purl identifier missing")
    private String purl;

    @Type(type = JsonTypes.JSON_BIN)
    @Column(name = "info", columnDefinition = JsonTypes.JSON_BIN)
    @ArtifactJsonProperty
    private JsonNode info;

}
