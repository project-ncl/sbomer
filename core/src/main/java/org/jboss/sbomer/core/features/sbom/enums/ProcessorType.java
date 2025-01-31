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
package org.jboss.sbomer.core.features.sbom.enums;

import java.util.Arrays;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;

@Getter
public enum ProcessorType {
    // TODO: Make it prettier!
    @JsonProperty("default")
    DEFAULT("default"), @JsonProperty("redhat-product")
    REDHAT_PRODUCT("redhat-product");

    final String slug;

    ProcessorType(String slug) {
        this.slug = slug;
    }

    public static Optional<ProcessorType> get(String slug) {
        return Arrays.stream(ProcessorType.values()).filter(impl -> impl.slug.equals(slug)).findFirst();
    }
}
