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
package org.jboss.sbomer.feature.sbom.core.validation;

import java.io.IOException;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.hibernate.validator.constraintvalidation.HibernateConstraintValidatorContext;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

public class ArtifactJsonPropertyValidator implements ConstraintValidator<ArtifactJsonProperty, JsonNode> {

    private ObjectMapper mapper = new ObjectMapper().enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);

    @Override
    public boolean isValid(JsonNode value, ConstraintValidatorContext context) {
        if (value == null) {
            context.unwrap(HibernateConstraintValidatorContext.class)
                    .addMessageParameter("errors", "missing artifact property");
            return false;
        }

        try {
            if (value instanceof ObjectNode) {
                mapper.readTree(((ObjectNode) value).asText());
            } else {
                mapper.readTree(((TextNode) value).toString());
            }

        } catch (IOException e) {
            context.unwrap(HibernateConstraintValidatorContext.class)
                    .addMessageParameter("errors", "unable to parse object");
            return false;
        }
        return true;
    }

}
