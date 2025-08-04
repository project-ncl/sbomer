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
package org.jboss.sbomer.service.nextgen.core.payloads.generation;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * <p>
 * Representation of the payload when requesting generations.
 * </p>
 *
 * @param eventId An (optional) event identifier within SBOMer under which the requested generations should be put on.
 * @param context Optional context related to the external generation request event.
 * @param requests List of generation requests.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GenerationsRequest(String eventId, @Valid ContextSpec context,
        @NotNull List<GenerationRequestSpec> requests) {
}