
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
package org.jboss.sbomer.feature.sbom.service.features.umb.producer;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;

import org.jboss.sbomer.core.errors.ApplicationException;
import org.jboss.sbomer.feature.sbom.service.features.umb.producer.model.GenerationFinishedMessageBody;

import io.vertx.core.json.JsonObject;
import io.vertx.json.schema.Draft;
import io.vertx.json.schema.JsonSchema;
import io.vertx.json.schema.JsonSchemaOptions;
import io.vertx.json.schema.OutputUnit;
import io.vertx.json.schema.Validator;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ApplicationScoped
public class GenerationFinishedMessageBodyValidator {

    public GenerationFinishedMessageBodyValidator() {
    }

    @Data
    @Builder
    public static class ValidationResult {
        boolean isValid;

        List<String> errors;

        public static ValidationResult fromOutputUnt(OutputUnit outputUnit) {
            return ValidationResult.builder()
                    .isValid(outputUnit.getValid())
                    .errors(
                            Optional.ofNullable(outputUnit.getErrors())
                                    .map(Collection::stream)
                                    .orElseGet(Stream::empty)
                                    .map(
                                            unit -> new StringBuilder().append(unit.getInstanceLocation())
                                                    .append(": ")
                                                    .append(unit.getError())
                                                    .toString())
                                    .toList())
                    .build();
        }
    }

    /**
     * A method to validate the content of the message body according to the defined JSON Schema.
     *
     * @return
     */
    public ValidationResult validate(GenerationFinishedMessageBody messageBody) {
        String messageStr = messageBody.toJson();
        String schema;

        try {
            InputStream is = getClass().getClassLoader().getResourceAsStream("message-success-schema.json");
            schema = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ApplicationException("Could not read the UMB message schema", e);
        }

        log.info("Validating: {}", messageStr);

        OutputUnit result = Validator
                .create(
                        JsonSchema.of(new JsonObject(schema)),
                        new JsonSchemaOptions().setBaseUri("https://jboss.org/sbomer/message.json")
                                .setDraft(Draft.DRAFT202012))
                .validate(new JsonObject(messageStr));

        return ValidationResult.fromOutputUnt(result);
    }

}
