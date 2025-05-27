package org.jboss.sbomer.service.feature.sbom.model.v1beta2;

import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "generation_status_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@RegisterForReflection
public class GenerationStatusHistory extends BaseStatusHistory {

    public GenerationStatusHistory(Generation generation, String status, String reason) {
        this.generation = generation;
        this.status = status;
        this.reason = reason;
    }

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "generation_id", nullable = false, updatable = false)
    private Generation generation;

    @Transactional
    public GenerationStatusHistory save() {
        persistAndFlush();
        return this;
    }
}