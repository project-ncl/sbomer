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
import java.util.Objects;

import org.jboss.sbomer.core.SchemaValidator;
import org.jboss.sbomer.core.SchemaValidator.ValidationResult;
import org.jboss.sbomer.core.errors.ApplicationException;
import org.jboss.sbomer.core.features.sbom.config.Config;
import org.jboss.sbomer.core.features.sbom.enums.GenerationRequestType;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * @author Marek Goldmann
 */
@ApplicationScoped
public class ConfigSchemaValidator implements Validator<Config> {
    /**
     * Performs validation of a give {@link Config} according to the JSON schema.
     *
     * @param config The {@link Config} object to validate.
     * @return a {@link ValidationResult} object.
     */
    @Override
    public ValidationResult validate(Config config) {
        if (config == null) {
            throw new ApplicationException("No configuration provided");
        }

        String schemaFile = GenerationRequestType.schemaFile(config.getClass());
        String name = "schemas/" + schemaFile;
        String schema;

        try (InputStream in = SchemaValidator.class.getClassLoader().getResourceAsStream(name)) {
            Objects.requireNonNull(in, "Resource " + name + " not found");
            schema = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ApplicationException("Could not read the configuration file schema", e);
        }

        return SchemaValidator.validate(schema, config.toJson());
    }
}
