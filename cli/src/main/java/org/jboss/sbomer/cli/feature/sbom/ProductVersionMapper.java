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
package org.jboss.sbomer.cli.feature.sbom;

import java.io.IOException;
import java.util.HashMap;
import java.util.Optional;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.sbomer.core.errors.ApplicationException;
import org.jboss.sbomer.core.features.sbom.config.PncBuildConfig;
import org.jboss.sbomer.core.features.sbom.utils.ObjectMapperProvider;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.Getter;

/**
 * <p>
 * Bean providing a mapping between a PNC Product Version and other systems for a given build.
 * </p>
 *
 * TODO: Ensure that PncBuildConfig is the only type we should support
 */
@ApplicationScoped
public class ProductVersionMapper {

    @ConfigProperty(name = "sbomer.pnc.product-mapping")
    Optional<String> mappingEnvironment;

    final ObjectMapper objectMapper = ObjectMapperProvider.yaml();

    public static class Mapping extends HashMap<String, PncBuildConfig> {

    }

    /**
     * <p>
     * The mapping between the product version defined in the PNC BuildConfig and other systems. The id being the key is
     * the PNC Product Version identifier.
     * </p>
     */
    @Getter
    Mapping mapping;

    @PostConstruct
    void init() {
        try {
            mapping = objectMapper.readValue(
                    getClass().getClassLoader()
                            .getResourceAsStream(
                                    "mapping/" + mappingEnvironment.orElse("prod") + "/product-mapping.yaml"),
                    Mapping.class);
        } catch (IOException e) {
            throw new ApplicationException("Could not read product mappings", e);
        }
    }
}
