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
package org.jboss.sbomer.service.rest.api.v1beta2.payloads.generation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * <p>
 * A single generation request.
 * </p>
 *
 * <p>
 * It covers a request to manifest a single deliverable which is identified by the {@code target} parameter. The
 * optional {@code config} parameter allows for customization of the generation process.
 * </p>
 *
 * @param target Information about the deliverable to manifest.
 * @param config Optional configuration for the generator that will take care of manifesting.
 */
public record GenerationRequestSpec(@NotNull @Valid TargetSpec target, @Valid GeneratorVersionConfigSpec generator) {
}