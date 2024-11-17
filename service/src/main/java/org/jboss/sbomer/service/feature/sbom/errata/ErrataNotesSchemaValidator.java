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
package org.jboss.sbomer.service.feature.sbom.errata;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.jboss.sbomer.core.SchemaValidator;
import org.jboss.sbomer.core.SchemaValidator.ValidationResult;
import org.jboss.sbomer.core.config.Validator;
import org.jboss.sbomer.core.errors.ApplicationException;
import org.jboss.sbomer.service.feature.sbom.errata.dto.Errata;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ErrataNotesSchemaValidator implements Validator<Errata> {

    /**
     * Performs validation of the notes of a given {@link Errata} according to the JSON schema.
     *
     * @param errata The {@link Errata} object to validate.
     * @return a {@link ValidationResult} object.
     */
    @Override
    public ValidationResult validate(Errata errata) {
        if (errata == null) {
            throw new ApplicationException("No errata advisory provided");
        }

        if (errata.getContent().getContent().getNotes() == null) {
            return ValidationResult.builder()
                    .withIsValid(false)
                    .withErrors(List.of("The errata advisory does not have notes"))
                    .build();
        }
        String schema;

        try (InputStream is = SchemaValidator.class.getClassLoader()
                .getResourceAsStream("schemas/appsvc-metadata.schema.json")) {
            schema = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ApplicationException("Could not read the configuration file schema", e);
        }

        return SchemaValidator.validate(schema, errata.getContent().getContent().getNotes().trim());
    }

}
