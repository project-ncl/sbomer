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
package org.jboss.sbomer.service.rest.api.v1beta2.payloads.management;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;

@Schema(description = "Payload to request the replay of an external event to be handled by a particular resolver.")
public record ReplayRequest(
        @NotBlank @Schema(
                description = "Identifier of the resolver type which supports particular external event.",
                example = "et-advisory") String resolver,

        @NotBlank @Schema(
                description = "The unique identifier of the external event known to particular resolver.",
                example = "1234") String identifier,

        @Schema(
                description = "Reason for initiating this replay. For audit purposes.",
                example = "Original event missed during system maintenance on 2024-01-10.") String reason) {
}