package org.jboss.sbomer.generation.status;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

/**
 * <p>
 * A Java representation of the manifest generation status Cloud Event.
 * </p>
 */
@Data
@EqualsAndHashCode(callSuper = false)
@SuperBuilder(setterPrefix = "with")
@Jacksonized
@JsonInclude(JsonInclude.Include.NON_NULL)
@RegisterForReflection
public class GenerationStatusEvent {

    public static final String TYPE_ID = "org.jboss.sbomer.generation.status.v1alpha1";

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
