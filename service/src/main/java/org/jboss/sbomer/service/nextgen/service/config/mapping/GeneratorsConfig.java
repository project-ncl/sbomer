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
package org.jboss.sbomer.service.nextgen.service.config.mapping;

import java.util.List;

public record GeneratorsConfig(List<GeneratorProfile> generatorProfiles,
        List<DefaultGeneratorMappingEntry> defaultGeneratorMappings) {
}

/**
 * Represents the default resource allocation for a generator version.
 */
record GeneratorResources(ResourceRequirement requests, ResourceRequirement limits) {
}

/**
 * Represents resource requests or limits (CPU, memory). Used within GeneratorResources.
 */
record ResourceRequirement(String cpu, String memory) {
}
