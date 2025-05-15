package org.jboss.sbomer.service.feature.sbom.model.v1beta2;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.jboss.sbomer.core.features.sbom.enums.GenerationRequestType;
import org.jboss.sbomer.core.features.sbom.enums.GenerationResult;
import org.jboss.sbomer.service.feature.sbom.k8s.model.SbomGenerationStatus;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.JsonNode;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
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
@ToString
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

    @Column(name = "created", nullable = false, updatable = false)
    private Instant created;

    @Column(name = "updated")
    private Instant updated;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    SbomGenerationStatus status; // TODO: rename enum

    @Column(name = "type", nullable = false)
    @Enumerated(EnumType.STRING)
    GenerationRequestType type; // TODO: rename enum

    @Column(name = "result")
    @Enumerated(EnumType.STRING)
    GenerationResult result;

    @Column(name = "identifier", nullable = false, updatable = false)
    String identifier;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "config")
    @ToString.Exclude
    @Schema(implementation = Map.class)
    private JsonNode config; // FIXME: I think this should be generic, to cover everything, even the future types

    @Column(name = "reason")
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    String reason;

    @ManyToMany(mappedBy = "generations", fetch = FetchType.LAZY)
    @JsonBackReference("generation-task")
    @Builder.Default
    private List<Task> tasks = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "otel_metadata")
    @ToString.Exclude
    @Schema(implementation = Map.class)
    private JsonNode otelMetadata;

    @PrePersist
    public void prePersist() {
        Instant now = Instant.now();

        if (this.created == null) {
            this.created = now;
        }

        this.setUpdated(now);
    }
}
