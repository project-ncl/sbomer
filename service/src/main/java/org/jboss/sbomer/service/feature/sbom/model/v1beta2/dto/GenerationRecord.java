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
package org.jboss.sbomer.service.feature.sbom.model.v1beta2.dto;

import java.time.Instant;

import org.jboss.sbomer.service.feature.sbom.model.v1beta2.Generation;
import org.jboss.sbomer.service.feature.sbom.model.v1beta2.enums.GenerationStatus;

import com.fasterxml.jackson.databind.JsonNode;

import io.quarkus.resteasy.reactive.links.RestLinkId;

/**
 * Representation of the {@link Generation} entity.
 */
public record GenerationRecord(@RestLinkId String id, Instant created, Instant updated, Instant finished,
        JsonNode request, GenerationStatus status, String result, String reason) {

}
