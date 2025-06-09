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
package org.jboss.sbomer.service.nextgen.core.dto.api;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Generator configuration.
 *
 * @param resources Generator resources in Kubernetes style. This is more a hint than actual values. Usage of these
 *        values depend on the generator
 * @param format Requested manifest format
 * @param options Custom options for generator
 */
public record GeneratorConfig(Resources resources, String format, JsonNode options) {
}