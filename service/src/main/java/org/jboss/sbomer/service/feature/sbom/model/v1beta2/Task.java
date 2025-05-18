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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.type.SqlTypes;
import org.jboss.sbomer.core.features.sbom.enums.TaskStatus;
import org.jboss.sbomer.core.features.sbom.utils.ObjectMapperProvider;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

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
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.Builder;
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
@Table(name = "task")
@Slf4j
@NoArgsConstructor
@AllArgsConstructor
@Builder(setterPrefix = "with")
@RegisterForReflection
public class Task extends PanacheEntityBase {
    @Id
    @Column(nullable = false, updatable = false)
    private String id;

    /**
     * Time when the task was created.
     */
    @Column(name = "created", nullable = false, updatable = false)
    private Instant created;

    /**
     * Last update time.
     */
    @Column(name = "updated")
    private Instant updated;

    /**
     * Time when all the work related to the task was finished (successfully or not).
     */
    @Column(name = "finished")
    private Instant finished;

    /**
     * Stores the source of the task.
     *
     * TODO: Do we care what is the source? In the eventing architecture everything will be an event. Maybe it is
     * relevant only for understanding the event field? See below.
     */
    @Column(name = "event_type", nullable = false)
    private String eventType;

    /**
     * Stores the event that triggered instantiated the task.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "event", nullable = false)
    @ToString.Exclude
    @Schema(implementation = Map.class)
    private JsonNode event;

    /**
     * Identifier of the status.
     */
    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private TaskStatus status;

    /**
     * A human-readable description of the status.
     */
    @Column(name = "reason")
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    String reason;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "config")
    @ToString.Exclude
    @Schema(implementation = Map.class)
    private JsonNode config; // FIXME: I think this should be generic, to cover everything, even the future types
                             // TODO: what is the task config? how it it

    /**
     * List of all generations related to the current Task.
     */
    @ManyToMany(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY) // TODO: this annotation
                                                                                              // has not well thought
                                                                                              // settings.
    @JoinTable(
            name = "task_generation",
            joinColumns = @JoinColumn(name = "task_id"),
            inverseJoinColumns = @JoinColumn(name = "generation_id"))
    @JsonManagedReference("task-generation")
    @Builder.Default
    private List<Generation> generations = new ArrayList<>();

    /**
     * Ensure time is set correctly before we save.
     */
    @PrePersist
    public void prePersist() {
        Instant now = Instant.now();

        if (this.created == null) {
            this.created = now;
        }

        this.setUpdated(now);
    }

    @Transactional
    public Task save() {

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

        Task task = (Task) o;
        return Objects.equals(id, task.id);
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
            log.warn("Creating JSON string out of the Task failed, defaulting to minimal representation", e);
        }

        return "Task[id=" + id + "]";
    }
}
