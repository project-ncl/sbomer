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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * <p>
 * Configuration of resources for requests and limits for a given execution.
 * </p>
 *
 * <p>
 * Generator may or may not take this into account. This is a suggestion. Please check documentation of a given
 * generator for more information.
 * </p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ResourcesSpec(ResourceRequirementSpec requests, ResourceRequirementSpec limits) {
}