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
package org.jboss.sbomer.service.feature.sbom.rest;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.validation.ConstraintViolation;

import org.hibernate.validator.engine.HibernateConstraintViolation;
import org.yaml.snakeyaml.parser.ParserException;

public class RestUtils {
    /**
     * Converts Hibernate Validator violations in a readable list of messages.
     *
     * @param violations
     * @return
     */
    public static List<String> constraintViolationsToMessages(Set<? extends ConstraintViolation<?>> violations) {
        List<String> errors = new ArrayList<>();

        violations.forEach(cv -> {
            List<String> payload = ((List<String>) cv.unwrap(HibernateConstraintViolation.class)
                    .getDynamicPayload(List.class));

            if (payload == null) {
                errors.add(cv.getPropertyPath().toString() + ": " + cv.getMessage());
                return;
            }
            // Dynamic payload contains list of error messages
            errors.addAll(payload.stream().map(error -> cv.getMessage() + ": " + error).toList());
        });

        return errors;
    }

    /**
     * Converts CycloneDX validation exceptions in a readable list of messages.
     *
     * @param violations
     * @return
     */
    public static List<String> parseExceptionsToMessages(List<ParserException> violations) {
        return violations.stream().map(cv -> "bom" + cv.getMessage().substring(1)).toList();
    }
}
