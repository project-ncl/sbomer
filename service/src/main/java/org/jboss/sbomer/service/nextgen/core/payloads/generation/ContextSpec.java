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

import java.time.Instant;

import jakarta.validation.constraints.NotBlank;

/**
 * <p>
 * The original context.
 * </p>
 *
 * <p>
 * In case we are responding to an <strong>external event</strong> that should result in generation(s), we should
 * encapsulate this information using {@link ContextSpec}.
 * </p>
 *
 * @param eventId Original, external event identifier.
 * @param system What is the source system of this event.
 * @param receivedAt What is the date and time when we initially received it.
 */
public record ContextSpec(@NotBlank String eventId, String system, Instant receivedAt, String payload) {
}