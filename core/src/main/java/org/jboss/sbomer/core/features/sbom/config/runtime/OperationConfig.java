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
package org.jboss.sbomer.core.features.sbom.config.runtime;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.jackson.Jacksonized;

/**
 * SBOMer configuration file for a particular PNC deliverable analysis.
 *
 * @author Andrea Vibelli
 */
@Getter
@Setter
@Builder(setterPrefix = "with")
@Jacksonized
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class OperationConfig {

    /**
     * The API version of the configuration file. In case of breaking changes this value will be used to detect the
     * correct (de)serializer.
     */
    @Builder.Default
    String apiVersion = "sbomer.jboss.org/v1alpha1";

    /**
     * Operation identifier of the deliverable analysis within PNC.
     */
    String operationId;

    /**
     * Configuration entries for the product.
     */
    ProductConfig product;

    /**
     * Deliverables entries for the given operation.
     */
    List<String> deliverableUrls;
}
