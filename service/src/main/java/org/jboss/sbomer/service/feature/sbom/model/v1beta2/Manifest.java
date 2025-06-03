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
package org.jboss.sbomer.service.feature.sbom.model.v1beta2;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.type.SqlTypes;
import org.jboss.sbomer.core.features.sbom.validation.CycloneDxBom;
import org.jboss.sbomer.service.feature.sbom.model.RandomStringIdGenerator;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@DynamicUpdate
@Getter
@Setter
@Entity
@ToString
@Table(name = "manifest")
@NoArgsConstructor
@AllArgsConstructor
@Builder(setterPrefix = "with")
@RegisterForReflection
public class Manifest extends PanacheEntityBase {

    @Id
    @Column(nullable = false, updatable = false)
    private String id;

    // @Column(name = "identifier", nullable = false, updatable = false)
    // @NotBlank(message = "Identifier missing")
    // private String identifier;

    /**
     * Time when the manifest was created.
     */
    @CreationTimestamp
    @Column(name = "created", nullable = false, updatable = false)
    private Instant created;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "sbom")
    @CycloneDxBom
    @ToString.Exclude
    @Schema(implementation = Map.class) // Workaround for swagger limitation of not being able to digest through a very
                                        // big schema which is the case if we use the Bom.class
    private JsonNode sbom;

    // @Column(name = "config_index")
    // private Integer configIndex;

    // @ManyToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    // @JoinColumn(foreignKey = @ForeignKey(name = "fk_sbom_generationrequest"))
    // private SbomGenerationRequest generationRequest;

    @JsonIgnore
    @Lob
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "status_msg")
    private String statusMessage;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "release_metadata")
    @ToString.Exclude
    @Schema(implementation = Map.class)
    private JsonNode releaseMetadata;

    @PrePersist
    protected void onPrePersist() {
        if (this.id == null) {
            this.id = RandomStringIdGenerator.generate();
        }
    }

    // /**
    // * Updates the purl for the object based on the SBOM content, if provided.
    // *
    // */
    // private void setupRootPurl() {
    // Bom bom = SbomUtils.fromJsonNode(getSbom());

    // rootPurl = null;

    // if (bom != null && bom.getMetadata() != null && bom.getMetadata().getComponent() != null) {
    // rootPurl = bom.getMetadata().getComponent().getPurl();
    // }
    // }

    // @PrePersist
    // public void prePersist() {
    // creationTime = Instant.now();
    // setupRootPurl();
    // }

    // @PreUpdate
    // public void preUpdate() {
    // setupRootPurl();
    // }

    @Transactional
    public Manifest save() {

        persistAndFlush();
        return this;
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null) {
            return false;
        }

        Class<?> oEffectiveClass = (o instanceof HibernateProxy proxy)
                ? proxy.getHibernateLazyInitializer().getPersistentClass()
                : o.getClass();
        Class<?> thisEffectiveClass = (this instanceof HibernateProxy proxy)
                ? proxy.getHibernateLazyInitializer().getPersistentClass()
                : this.getClass();

        if (thisEffectiveClass != oEffectiveClass) {
            return false;
        }

        Manifest sbom1 = (Manifest) o;
        return Objects.equals(id, sbom1.id);
    }

    @Override
    public final int hashCode() {
        return Objects.hash(id);
    }
}
