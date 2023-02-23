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
package org.redhat.sbomer.validation.exceptions;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.validation.ConstraintViolation;

import org.cyclonedx.exception.ParseException;

import lombok.Getter;

@Getter
public class ValidationException extends RuntimeException {
    List<String> messages;

    public ValidationException(String message) {
        super(message);
        this.messages = Collections.singletonList(message);
    }

    /**
     * Converts Hibernate Validator violations in a readable list of messages.
     *
     * @param violations
     */
    public ValidationException(Set<? extends ConstraintViolation<?>> violations) {
        messages = violations.stream().map(cv -> cv.getPropertyPath() + ": " + cv.getMessage()).toList();
    }

    /**
     * Converts CycloneDX validation exceptions in a readable list of messages.
     *
     * @param violations
     */
    public ValidationException(List<ParseException> violations) {
        messages = violations.stream().map(cv -> "bom" + cv.getMessage().substring(1)).toList();
    }

}
