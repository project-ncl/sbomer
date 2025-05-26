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
@Table(name = "event_status_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@RegisterForReflection
public class EventStatusHistory extends BaseStatusHistory {

    public EventStatusHistory(Event event, String status, String reason) {
        this.event = event;
        this.status = status;
        this.reason = reason;
    }

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false, updatable = false)
    private Event event;

    @Transactional
    public EventStatusHistory save() {
        persistAndFlush();
        return this;
    }
}