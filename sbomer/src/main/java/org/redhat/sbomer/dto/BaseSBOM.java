package org.redhat.sbomer.dto;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@ToString
@EqualsAndHashCode
@JsonDeserialize(builder = BaseSBOM.Builder.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public class BaseSBOM {

    private final String id;

    private final String buildId;

    private final Instant generationTime;

    private final JsonNode bom;

    @lombok.Builder(builderClassName = "Builder", toBuilder = true)
    private BaseSBOM(String id, String buildId, Instant generationTime, JsonNode bom) {
        this.id = id;
        this.buildId = buildId;
        this.generationTime = generationTime;
        this.bom = bom;
    }

    @JsonPOJOBuilder(withPrefix = "")
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class Builder {
    }

}
