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
package org.jboss.sbomer.core.errors;

import java.util.List;
import java.util.UUID;

import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class ClientException extends ApplicationException {
    private static final int DEFAULT_CODE = 400;

    private final List<String> errors;

    final String errorId;

    public int getCode() {
        return DEFAULT_CODE;
    }

    public ClientException(String message, Object... params) {
        super(message, params);
        this.errors = null;
        this.errorId = UUID.randomUUID().toString();
    }

    public ClientException(String message, List<String> errors, Object... params) {
        super(message, params);
        this.errors = errors;
        this.errorId = UUID.randomUUID().toString();
    }
}
