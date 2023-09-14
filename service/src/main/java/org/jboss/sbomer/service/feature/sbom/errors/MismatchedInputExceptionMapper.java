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
package org.jboss.sbomer.service.feature.sbom.errors;

import java.util.UUID;

import org.jboss.sbomer.core.errors.ErrorResponse;

import com.fasterxml.jackson.databind.exc.MismatchedInputException;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import lombok.extern.slf4j.Slf4j;

/**
 * Handles cases where the received content cannot be deserialized.
 */
@Provider
@Slf4j
public class MismatchedInputExceptionMapper implements ExceptionMapper<MismatchedInputException> {
    @Override
    public Response toResponse(MismatchedInputException e) {
        log.error("Received content that cannot be deserialized");

        Response.ResponseBuilder builder = Response.status(400);

        ErrorResponse error = ErrorResponse.builder()
                .errorId(UUID.randomUUID().toString())
                .errorType(e.getClass().getSimpleName())
                .message("An error occurred while deserializing provided content: " + e.getOriginalMessage())
                .build();

        return builder.entity(error).type(MediaType.APPLICATION_JSON).build();

    }

}
