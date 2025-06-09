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
package org.jboss.sbomer.service.nextgen.core.events;

import org.jboss.sbomer.service.nextgen.core.dto.api.GenerationRequest;
import org.jboss.sbomer.service.nextgen.core.dto.model.EventRecord;
import org.jboss.sbomer.service.nextgen.core.dto.model.GenerationRecord;
import org.jboss.sbomer.service.nextgen.core.utils.JacksonUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * Event fired after a Generation is scheduled.
 */
@Slf4j
public record GenerationScheduledEvent(EventRecord event, GenerationRecord generation) {

    public boolean isOfRequestType(String type) {
        if (this.generation == null) {
            log.warn("Generation was not populated");
            return false;
        }

        if (this.generation.request() == null) {
            log.warn("Request was not provided");
            return false;
        }

        GenerationRequest request = JacksonUtils.parse(GenerationRequest.class, generation.request());

        if (request.target().type().equals(type)) {
            return true;
        }

        return false;
    }
}
