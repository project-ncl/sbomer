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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;
import org.jboss.sbomer.core.features.sbom.utils.ObjectMapperProvider;
import org.jboss.sbomer.service.nextgen.core.enums.GenerationResult;
import org.jboss.sbomer.service.nextgen.core.enums.GenerationStatus;
import org.slf4j.helpers.MessageFormatter;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.f4b6a3.tsid.TsidCreator;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@JsonInclude(Include.NON_NULL)
@DynamicUpdate
@Getter
@Setter
@Entity
@Table(name = "generation")
@Slf4j
@NoArgsConstructor
@AllArgsConstructor
@Builder(setterPrefix = "with")
@RegisterForReflection
public class Generation extends PanacheEntityBase {
    @Id
    @Column(nullable = false, updatable = false)
    private String id;

    /**
     * Time when the generation was created.
     */
    @CreationTimestamp
    @Column(name = "created", nullable = false, updatable = false)
    private Instant created;

    /**
     * Last update time.
     */
    @UpdateTimestamp
    @Column(name = "updated")
    private Instant updated;

    /**
     * Time when all the work related to the generation was finished (successfully or not).
     */
    @Column(name = "finished")
    private Instant finished;

    @ManyToOne
    private Generation parent;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "request")
    @ToString.Exclude
    private ObjectNode request;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata")
    @ToString.Exclude
    private Map<String, String> metadata;

    /**
     * Identifier of the status.
     */
    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    @Default
    GenerationStatus status = GenerationStatus.NEW;

    /**
     * <p>
     * Identifier of the result.
     * </p>
     *
     * <p>
     * Useful when we want to programmatically understand what was the reason for the failure. This, together with
     * {@link Generation#reason} provides full picture.
     * </p>
     */
    @Column(name = "result")
    @Enumerated(EnumType.STRING)
    GenerationResult result; // TODO: we may need to refresh the list. These are currently specific to the tools
                             // that are executed, maybe this should be a string as well?

    /**
     * A human-readable description of the status.
     */
    @Column(name = "reason")
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    String reason;

    @ManyToMany(mappedBy = "generations", fetch = FetchType.LAZY)
    @JsonBackReference("generation-event")
    @Builder.Default
    private List<Event> events = new ArrayList<>();

    @OneToMany(mappedBy = "generation", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("timestamp ASC") // Ensures history is ordered chronologically
    @Builder.Default
    @JsonManagedReference
    private List<GenerationStatusHistory> statuses = new ArrayList<>();

    @OneToMany(mappedBy = "generation", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    @JsonManagedReference
    private List<Manifest> manifests = new ArrayList<>();

    public void setStatus(GenerationStatus status) {
        // This is a workaround so that when we update the status of the entity later, the status history entity will be
        // created as well.
        statuses.size();
        this.status = status;
    }

    public void setReason(String reason, Object... params) {
        this.reason = MessageFormatter.arrayFormat(reason, params).getMessage();
    }

    @PrePersist
    protected void onPrePersist() {
        if (this.id == null) {
            this.id = "G" + TsidCreator.getTsid1024().toString();
        }

        statuses.add(new GenerationStatusHistory(this, status, reason));
    }

    @PreUpdate
    protected void onPreUpdate() {
        statuses.add(new GenerationStatusHistory(this, status, reason));
    }

    @Transactional
    public Generation save() {

        persistAndFlush();
        return this;
    }

    @Override
    public String toString() {
        try {
            return ObjectMapperProvider.json().writeValueAsString(this);
        } catch (JsonProcessingException e) {
            log.warn("Creating JSON string out of the Generation failed, defaulting to minimal representation", e);
        }

        return "Generation[id=" + id + "]";
    }
}
