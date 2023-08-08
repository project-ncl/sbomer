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

import lombok.Getter;

public enum GenerationResult {
    SUCCESS(0),
    ERR_GENERAL(1),
    ERR_CONFIG_INVALID(2),
    ERR_CONFIG_MISSING(3),
    ERR_INDEX_INVALID(4),
    ERR_GENERATION(5),
    ERR_SYSTEM(99),
    ERR_MULTI(100);

    @Getter
    int code;

    GenerationResult(int code) {
        this.code = code;
    }

    public static Optional<GenerationResult> fromCode(int code) {
        return Arrays.stream(GenerationResult.values()).filter(r -> r.code == code).findFirst();

    }
}
