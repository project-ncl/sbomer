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
package org.jboss.sbomer.service.nextgen.core.dto.model;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

import org.jboss.sbomer.service.nextgen.core.dto.api.GenerationRequest;
import org.jboss.sbomer.service.nextgen.core.enums.GenerationResult;
import org.jboss.sbomer.service.nextgen.core.enums.GenerationStatus;
import org.jboss.sbomer.service.nextgen.core.utils.JacksonUtils;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.quarkus.resteasy.reactive.links.RestLinkId;
import lombok.extern.slf4j.Slf4j;

/**
 * Representation of the Generation entity.
 */
@Slf4j
public record GenerationRecord(@RestLinkId String id, Instant created, Instant updated, Instant finished,
        ObjectNode request, Map<String, String> metadata, GenerationStatus status, GenerationResult result,
        String reason) {

    public boolean isSupported(Set<String> types) {
        if (request() == null) {
            log.warn("Request was not provided");
            return false;
        }

        GenerationRequest generationRequest = JacksonUtils.parse(GenerationRequest.class, request());

        for (String type : types) {
            if (generationRequest.target().type().equals(type)) {
                return true;
            }
        }

        return false;
    }
}
