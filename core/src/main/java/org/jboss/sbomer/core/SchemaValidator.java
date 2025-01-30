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
package org.jboss.sbomer.core;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import io.vertx.core.json.JsonObject;
import io.vertx.json.schema.Draft;
import io.vertx.json.schema.JsonSchema;
import io.vertx.json.schema.JsonSchemaOptions;
import io.vertx.json.schema.OutputFormat;
import io.vertx.json.schema.OutputUnit;
import io.vertx.json.schema.Validator;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SchemaValidator {

    private SchemaValidator() {
        // This is a utlity class and should not be instantiated
    }

    @Data
    @Builder(setterPrefix = "with")
    public static class ValidationResult {
        boolean isValid;

        List<String> errors;

        public static ValidationResult fromOutputUnit(OutputUnit outputUnit) {
            return ValidationResult.builder()
                    .withIsValid(outputUnit.getValid())
                    .withErrors(
                            Optional.ofNullable(outputUnit.getErrors())
                                    .map(Collection::stream)
                                    .orElseGet(Stream::empty)
                                    .map(unit -> unit.getInstanceLocation() + ": " + unit.getError())
                                    .toList())
                    .build();
        }
    }

    /**
     * A method to validate the content of the message body according to the defined JSON Schema.
     *
     * @return
     */
    public static ValidationResult validate(String schema, String body) {
        log.debug("Validating: {}", body);
        log.trace("Schema: {}", schema);

        OutputUnit result = Validator
                .create(
                        JsonSchema.of(new JsonObject(schema)),
                        new JsonSchemaOptions().setBaseUri("https://jboss.org/sbomer")
                                .setOutputFormat(OutputFormat.Basic)
                                .setDraft(Draft.DRAFT202012))
                .validate(new JsonObject(body));

        ValidationResult validationResult = ValidationResult.fromOutputUnit(result);

        if (!validationResult.isValid()) {
            log.error("Validation failed!");

            validationResult.getErrors().forEach(msg -> log.error(msg));
        }

        return validationResult;
    }

}
