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
package org.jboss.sbomer.core.dto.v1beta1;

import java.time.Instant;
import java.util.Map;

import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.jboss.sbomer.core.config.request.RequestConfig;
import org.jboss.sbomer.core.features.sbom.enums.RequestEventStatus;
import org.jboss.sbomer.core.features.sbom.enums.RequestEventType;

import com.fasterxml.jackson.databind.JsonNode;

@Schema(name = "V1BaseBeta1RequestRecord")
public record V1BaseBeta1RequestRecord (
    String id,
    Instant receivalTime,
    RequestEventType eventType,
    RequestEventStatus eventStatus,
    String reason,
    @Schema(implementation = Map.class) RequestConfig requestConfig,
    @Schema(implementation = Map.class) JsonNode event) {
}