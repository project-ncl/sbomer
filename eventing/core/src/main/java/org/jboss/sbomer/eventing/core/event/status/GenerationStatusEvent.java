package org.jboss.sbomer.eventing.core.event.status;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

@Data
@EqualsAndHashCode(callSuper = false)
@SuperBuilder(setterPrefix = "with")
@Jacksonized
@JsonInclude(JsonInclude.Include.NON_NULL)
@RegisterForReflection
public class GenerationStatusEvent {

    @Data
    @SuperBuilder(setterPrefix = "with")
    @Jacksonized
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @RegisterForReflection
    public static class Metadata {
    }

    @Data
    @Builder(setterPrefix = "with")
    @Jacksonized
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @RegisterForReflection
    public static class Spec {
        private GenerationStatus status;
        private String reason;
    }

    private Metadata metadata;
    private Spec spec;
}
