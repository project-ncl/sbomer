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
package org.jboss.sbomer.core.features.sbom.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jboss.sbomer.core.features.sbom.config.runtime.ProductConfig;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeName;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

/**
 * SBOMer configuration file for a particular PNC build. This class represents the configuration file that can be added
 * at the {@code .sbomer/config.yaml} path in the source code repository used to build the project.
 *
 * @author Marek Goldmann
 */
@Data
@SuperBuilder(setterPrefix = "with")
@Jacksonized
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonTypeName("pnc-build")
public class PncBuildConfig extends Config {
    /**
     * Build identifier within PNC.
     */
    String buildId;

    /**
     * Build environment (tools).
     */
    Map<String, String> environment;

    /**
     * List of configuration entries for products.
     */
    @Builder.Default
    List<ProductConfig> products = new ArrayList<>();

    @JsonIgnore
    @Override
    public boolean isEmpty() {
        return this.equals(new PncBuildConfig());
    }
}
