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

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Supported generator implementations.
 *
 * @author Marek Goldmann
 */
public enum GeneratorType {
    @JsonProperty("maven-cyclonedx")
    MAVEN_CYCLONEDX, @JsonProperty("maven-domino")
    MAVEN_DOMINO, @JsonProperty("gradle-cyclonedx")
    GRADLE_CYCLONEDX, @JsonProperty("nodejs-cyclonedx")
    NODEJS_CYCLONEDX, @JsonProperty("cyclonedx-operation")
    CYCLONEDX_OPERATION
}
