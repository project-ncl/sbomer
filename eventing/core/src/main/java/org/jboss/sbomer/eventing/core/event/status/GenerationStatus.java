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
package org.jboss.sbomer.eventing.core.event.status;

import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * TODO: This is a copy of {@link org.jboss.sbomer.service.feature.sbom.k8s.model.SbomGenerationStatus}.
 */
@RegisterForReflection
public enum GenerationStatus {
    NO_OP, NEW, INITIALIZING, INITIALIZED, GENERATING, FINISHED, FAILED;

    public static GenerationStatus fromName(String phase) {
        return GenerationStatus.valueOf(phase.toUpperCase());
    }

    public String toName() {
        return this.name().toLowerCase();
    }

    public boolean isOlderThan(GenerationStatus desiredStatus) {
        if (desiredStatus == null || desiredStatus.equals(NO_OP)) {
            return false;
        }

        return desiredStatus.ordinal() > this.ordinal();
    }

    public boolean isFinal() {
        return this.equals(FAILED) || this.equals(FINISHED);
    }
}
