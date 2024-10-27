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

import java.util.List;

import org.jboss.sbomer.core.features.sbom.config.BrewRPMConfig;
import org.jboss.sbomer.core.features.sbom.config.AdvisoryConfig;
import org.jboss.sbomer.core.features.sbom.config.Config;
import org.jboss.sbomer.core.features.sbom.config.DeliverableAnalysisConfig;
import org.jboss.sbomer.core.features.sbom.config.OperationConfig;
import org.jboss.sbomer.core.features.sbom.config.PncBuildConfig;
import org.jboss.sbomer.core.features.sbom.config.SyftImageConfig;

import lombok.Getter;

public enum GenerationRequestType {
    BUILD(PncBuildConfig.class, "config.json"),
    OPERATION(OperationConfig.class, "operation_config.json"),
    CONTAINERIMAGE(SyftImageConfig.class, "syft-image-config.json"),
    ANALYSIS(DeliverableAnalysisConfig.class, "deliverable_analysis_config.json"),
    BREW_RPM(BrewRPMConfig.class, "brew_rpm_config.json"),
    ADVISORY(AdvisoryConfig.class, "advisory-config.json");

    @Getter
    Class<? extends Config> implementation;

    @Getter
    String schema;

    GenerationRequestType(Class<? extends Config> implementation, String schema) {
        this.implementation = implementation;
        this.schema = schema;
    }

    public static GenerationRequestType fromName(String type) {
        return GenerationRequestType.valueOf(type.toUpperCase().replace("-", "_"));
    }

    public static String schemaFile(Class<? extends Config> clazz) {
        GenerationRequestType type = List.of(GenerationRequestType.values())
                .stream()
                .filter(t -> t.getImplementation().equals(clazz))
                .findFirst()
                .orElse(null);

        if (type == null) {
            return null;
        }

        return type.getSchema();
    }

    public String toName() {
        return this.name().toLowerCase().replace("_", "-");
    }

}
