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
package org.jboss.sbomer.service.nextgen.core.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.jboss.sbomer.service.nextgen.service.model.Event;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Representation of the {@link Event} entity.
 */
public record EventRecord(String id, EventRecord parent, Instant created, Instant updated, Instant finished,
        Map<String, String> metadata, JsonNode request, List<GenerationRecord> generations, String status,
        String reason) {

}
