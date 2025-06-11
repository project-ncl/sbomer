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

import org.jboss.sbomer.service.nextgen.core.enums.GenerationStatus;
import org.slf4j.helpers.MessageFormatter;

import jakarta.validation.constraints.NotNull;

/**
 * <p>
 * Payload used to update SBOMer with information about the progress of the generation.
 * </p>
 *
 * <p>
 * This endpoint is used only by workers (generators).
 * </p>
 * 
 * TODO: This probably should not exist and we should send the full FenerationRecord, manifests should be handled in a different call maybe
 *
 * @param status The status identifier.
 * @param result A programmatic result information.
 * @param reason A human-readable description of the current status.
 * @param manifests List of manifest identifiers.
 */
public record UpdatePayload(@NotNull GenerationStatus status, String result, String reason, List<String> manifests) {

    public static UpdatePayload of(GenerationStatus status, String reason, Object... params) {
        return new UpdatePayload(status, null, MessageFormatter.arrayFormat(reason, params).getMessage(), null);
    }
}