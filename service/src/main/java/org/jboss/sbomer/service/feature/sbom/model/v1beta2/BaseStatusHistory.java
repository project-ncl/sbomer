package org.jboss.sbomer.service.feature.sbom.model.v1beta2;

import java.time.Instant;
import java.util.Objects;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.jboss.sbomer.service.feature.sbom.model.RandomStringIdGenerator;

import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@MappedSuperclass
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder(setterPrefix = "with")
@RegisterForReflection
public abstract class BaseStatusHistory {

    @Id
    @Column(nullable = false, updatable = false)
    private String id;

    @CreationTimestamp
    @Column(name = "timestamp", nullable = false, updatable = false)
    private Instant timestamp;

    @Column(name = "status", nullable = false, updatable = false)
    private String status;

    @Column(name = "reason", updatable = false)
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    private String reason;

    @Column(name = "changed_by", updatable = false)
    private String changedBy;

    @PrePersist
    protected void onPrePersist() {
        if (this.id == null) {
            this.id = RandomStringIdGenerator.generate();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;

        if (o == null || getClass() != o.getClass())
            return false;

        BaseStatusHistory that = (BaseStatusHistory) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}