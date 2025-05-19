package org.jboss.sbomer.service.feature.sbom.model.v1beta2.dto;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({ "id", "parent", "created", "updated", "finished" })
public record EventRecord(String id, Instant created, Instant updated, Instant finished, String identifier,
        EventRecord parent, String source, String status) {

}
