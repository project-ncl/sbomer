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
package org.jboss.sbomer.service.nextgen.core.dto.request;

import org.jboss.sbomer.core.errors.ApplicationException;
import org.jboss.sbomer.core.features.sbom.utils.ObjectMapperProvider;
import org.jboss.sbomer.service.nextgen.core.dto.GenerationRecord;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Represents a single generation request. This object is used to encapsulate information that is required to start a
 * generation of manifest for a single deliverable.
 */
public record GenerationRequest(Generator generator, Target target) {

    /**
     * Converts the {@code request} field from {@link GenerationRecord} which is a {@link ObjectNode} into a
     * {@link GenerationRequest}.
     * 
     * @param generation
     * @return Converted {@link GenerationRequest} object.
     */
    public static GenerationRequest parse(GenerationRecord generation) {
        try {
            return ObjectMapperProvider.json().treeToValue(generation.request(), GenerationRequest.class);
        } catch (JsonProcessingException e) {
            throw new ApplicationException("Unable to parse provided resource configuration", e);
        }
    }
}
