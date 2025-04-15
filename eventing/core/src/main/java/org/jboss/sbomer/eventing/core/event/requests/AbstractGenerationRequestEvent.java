package org.jboss.sbomer.eventing.core.event.requests;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

@Data
@EqualsAndHashCode(callSuper = false)
@SuperBuilder(setterPrefix = "with")
@JsonInclude(JsonInclude.Include.NON_NULL)
@RegisterForReflection
public abstract class AbstractGenerationRequestEvent implements GenerationRequestEvent {
    @Data
    @SuperBuilder(setterPrefix = "with")
    @Jacksonized
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Metadata {
    }

    private Metadata metadata;
}
