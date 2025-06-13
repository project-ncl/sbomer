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
package org.jboss.sbomer.service.nextgen.service.model;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.type.SqlTypes;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.f4b6a3.tsid.TsidCreator;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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

    /**
     * Time when the manifest was created.
     */
    @CreationTimestamp
    @Column(name = "created", nullable = false, updatable = false)
    private Instant created;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "bom")
    @ToString.Exclude
    @Schema(implementation = Map.class)
    private JsonNode bom;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "generation_id", nullable = false, updatable = false)
    @JsonBackReference
    private Generation generation;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata")
    @ToString.Exclude
    @Schema(implementation = Map.class)
    private JsonNode metadata;

    @PrePersist
    protected void onPrePersist() {
        if (this.id == null) {
            this.id = TsidCreator.getTsid1024().toString();
        }
    }

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
