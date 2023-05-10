/**
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
package org.jboss.sbomer.core.service;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.sbomer.core.config.ProductConfig;
import org.jboss.sbomer.core.errors.ApplicationException;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Getter;

/**
 * <p>
 * Bean providing a mapping between a PNC Product Version and other systems for a given build.
 * </p>
 *
 */
@ApplicationScoped
public class ProductVersionMapper {

    @ConfigProperty(name = "sbomer.pnc.product-mapping.environment")
    String mappingEnvironment;

    public static class Mapping extends HashMap<String, ProductConfig> {

    }

    /**
     * <p>
     * The mapping between the product version defined in the PNC BuildConfig and other systems. The id being the key is
     * the PNC Product Version identifier.
     * </p>
     */
    @Getter
    Map<String, ProductConfig> mapping;

    @PostConstruct
    void init() {
        try {
            mapping = new ObjectMapper().readValue(
                    getClass().getClassLoader().getResourceAsStream(mappingEnvironment + "/product-mapping.json"),
                    Mapping.class);
        } catch (IOException e) {
            throw new ApplicationException("Could not read product mappings", e);
        }
    }
}