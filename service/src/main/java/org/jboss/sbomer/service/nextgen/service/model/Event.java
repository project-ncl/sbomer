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
import java.util.Objects;

import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.type.SqlTypes;
import org.jboss.sbomer.core.features.sbom.utils.ObjectMapperProvider;
import org.jboss.sbomer.service.nextgen.core.enums.EventStatus;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
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
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
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
@Table(name = "event")
@Slf4j
@NoArgsConstructor
@AllArgsConstructor
@Builder(setterPrefix = "with")
@RegisterForReflection
public class Event extends PanacheEntityBase {
    @Id
    @Column(nullable = false, updatable = false)
    private String id;

    /**
     * Time when the event was created.
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
     * Time when all the work related to the event was finished (successfully or not).
     */
    @Column(name = "finished")
    private Instant finished;

    /**
     * In case of retries, this filed will be populated to make it easy to understand what was the parent event.
     */
    @ManyToOne
    private Event parent;

    /**
     * <p>
     * Event metadata.
     * </p>
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", nullable = false)
    @Schema(implementation = Map.class)
    private Map<String, String> metadata;

    /**
     * <p>
     * Optional original request content.
     * </p>
     *
     * <p>
     * Content depends on the {@code metadata.source} and it may not be populated at all. This content is used to help
     * trace events.
     * </p>
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "request")
    @ToString.Exclude
    @Schema(implementation = Map.class)
    private JsonNode request;

    /**
     * Identifier of the status.
     */
    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    @Default
    private EventStatus status = EventStatus.NEW;

    /**
     * A human-readable description of the status.
     */
    @Column(name = "reason")
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    String reason;

    /**
     * List of all generations related to the current Event.
     */
    @ManyToMany(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
    @JoinTable(
            name = "event_generation",
            joinColumns = @JoinColumn(name = "event_id"),
            inverseJoinColumns = @JoinColumn(name = "generation_id"))
    @JsonManagedReference("event-generation")
    @Builder.Default
    private List<Generation> generations = new ArrayList<>();

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("timestamp ASC")
    @Builder.Default
    @JsonManagedReference
    private List<EventStatusHistory> statuses = new ArrayList<>();

    public void setStatus(EventStatus status) {
        // This is a workaround so that when we update the status of the entity later, the status history entity will be
        // created as well.
        statuses.size();
        this.status = status;
    }

    @PrePersist
    protected void onPrePersist() {
        if (this.id == null) {
            this.id = "E" + TsidCreator.getTsid1024().toString();
        }

        statuses.add(new EventStatusHistory(this, status, reason));
    }

    @PreUpdate
    protected void onPreUpdate() {
        statuses.add(new EventStatusHistory(this, status, reason));
    }

    @Transactional
    public Event save() {

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

        Event event = (Event) o;
        return Objects.equals(id, event.id);
    }

    @Override
    public final int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        try {
            return ObjectMapperProvider.json().writeValueAsString(this);
        } catch (JsonProcessingException e) {
            log.warn("Creating JSON string out of the Event failed, defaulting to minimal representation", e);
        }

        return "Event[id=" + id + "]";
    }
}
