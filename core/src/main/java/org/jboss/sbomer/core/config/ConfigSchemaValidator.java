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
package org.jboss.sbomer.core.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.jboss.sbomer.core.SchemaValidator;
import org.jboss.sbomer.core.SchemaValidator.ValidationResult;
import org.jboss.sbomer.core.errors.ApplicationException;
import org.jboss.sbomer.core.features.sbom.config.runtime.Config;
import org.jboss.sbomer.core.features.sbom.utils.ObjectMapperProvider;

import com.fasterxml.jackson.core.JsonProcessingException;

/**
 * @author Marek Goldmann
 */
public class ConfigSchemaValidator {
    /**
     * Performs validation of a give {@link Config} according to the JSON schema.
     *
     * @param config The {@link Config} object to validate.
     * @return a {@link ValidationResult} object.
     */
    public static ValidationResult validate(Config config) {
        String schema;

        try {
            InputStream is = SchemaValidator.class.getClassLoader().getResourceAsStream("schemas/config.json");
            schema = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ApplicationException("Could not read the configuration file schema", e);
        }

        try {
            return SchemaValidator.validate(schema, ObjectMapperProvider.json().writeValueAsString(config));
        } catch (JsonProcessingException e) {
            throw new ApplicationException("An error occurred while converting configuration file into JSON", e);
        }
    }
}
