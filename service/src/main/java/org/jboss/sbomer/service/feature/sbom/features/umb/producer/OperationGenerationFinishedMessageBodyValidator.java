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
package org.jboss.sbomer.service.feature.sbom.features.umb.producer;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.jboss.sbomer.core.SchemaValidator;
import org.jboss.sbomer.core.SchemaValidator.ValidationResult;
import org.jboss.sbomer.core.config.Validator;
import org.jboss.sbomer.core.errors.ApplicationException;
import org.jboss.sbomer.service.feature.sbom.features.umb.producer.model.OperationGenerationFinishedMessageBody;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class OperationGenerationFinishedMessageBodyValidator
        implements Validator<OperationGenerationFinishedMessageBody> {

    public OperationGenerationFinishedMessageBodyValidator() {
    }

    @Override
    public ValidationResult validate(OperationGenerationFinishedMessageBody messageBody) {
        if (messageBody == null) {
            throw new ApplicationException("No message to validate provided");
        }

        String schema;

        try {
            InputStream is = getClass().getClassLoader()
                    .getResourceAsStream("schemas/message-operation-success-schema.json");
            schema = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ApplicationException("Could not read the configuration file schema", e);
        }

        return SchemaValidator.validate(schema, messageBody.toJson());
    }

}
