/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2023 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.sbomer.eventing.core.event.requests.zip;

import org.jboss.sbomer.eventing.core.event.requests.AbstractGenerationRequestEvent;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeName;

import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

/**
 * <p>
 * Representation of the {@code org.jboss.sbomer.generation.request.image.v1alpha1} Cloud Event.
 * </p>
 *
 * <p>
 * This event requests a manifest for a container image.
 * </p>
 *
 * ce-type: {@code org.jboss.sbomer.generation.request.image.v1alpha1}
 */
@Data
@EqualsAndHashCode(callSuper = false)
@SuperBuilder(setterPrefix = "with")
@Jacksonized
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonTypeName("org.jboss.sbomer.generation.request.image.v1alpha1")
@RegisterForReflection
public class GenerationRequestImageV1Alpha1Event extends AbstractGenerationRequestEvent {

    @Data
    @SuperBuilder(setterPrefix = "with")
    @Jacksonized
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Spec {
        private String image;
    }

    private Spec spec;
}
