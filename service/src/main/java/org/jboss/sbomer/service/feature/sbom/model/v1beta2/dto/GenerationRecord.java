package org.jboss.sbomer.service.feature.sbom.model.v1beta2.dto;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import io.quarkus.resteasy.reactive.links.RestLinkId;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder(value = { "id", "created", "updated", "finished" })
public record GenerationRecord(@JsonProperty(index = 0, value = "id") @RestLinkId String id,
        @JsonProperty(index = 1) Instant created, @JsonProperty(index = 2) Instant updated,
        @JsonProperty(index = 3) Instant finished) {

}
