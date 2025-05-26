package org.jboss.sbomer.service.feature.sbom.model.v1beta2;

import java.time.Instant;
import java.util.Objects;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.type.SqlTypes;
import org.jboss.sbomer.service.feature.sbom.model.RandomStringIdGenerator;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@MappedSuperclass
@Getter
@Setter
@NoArgsConstructor
@RegisterForReflection
public abstract class BaseStatusHistory extends PanacheEntityBase {
    @Id
    @Column(nullable = false, updatable = false)
    private String id;

    @CreationTimestamp
    @Column(name = "timestamp", nullable = false, updatable = false)
    private Instant timestamp;

    @Column(name = "status", nullable = false, updatable = false)
    protected String status;

    @Column(name = "reason", updatable = false)
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    protected String reason;

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

        if (o == null)
            return false;

        Class<?> oEffectiveClass = (o instanceof HibernateProxy proxy)
                ? proxy.getHibernateLazyInitializer().getPersistentClass()
                : o.getClass();
        Class<?> thisEffectiveClass = (this instanceof HibernateProxy proxy)
                ? proxy.getHibernateLazyInitializer().getPersistentClass()
                : this.getClass();

        if (thisEffectiveClass != oEffectiveClass) {
            return false;
        }

        BaseStatusHistory that = (BaseStatusHistory) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}