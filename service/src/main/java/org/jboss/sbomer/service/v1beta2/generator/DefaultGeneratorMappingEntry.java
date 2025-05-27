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
package org.jboss.sbomer.service.v1beta2.generator;

import java.util.List;

/**
 * Represents a single mapping from a content type to a list of preferred generators.
 *
 * @param type the type of the content to manifest, for example {@code CONTAINER_IMAGE} or {@code MAVEN_PROJECT}
 * @param generators a list of generators that support given type. First in the list is the preferred generator.
 */
public record DefaultGeneratorMappingEntry(String targetType, List<String> generators) {
}