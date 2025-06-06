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

import java.util.Map;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * Options for a given generator used for generating manifests.
 *
 * @param format The manifest output format.
 * @param resources Resource requirements related to execution phase for a current generation.
 * @param options Custom, generator(version)-specific, options which should be applied to the generation process.
 */
public record GeneratorConfigSpec(String format, ResourcesSpec resources,
        @Schema(description = "Specific options for a particular generator version.") Map<String, Object> options) {
}