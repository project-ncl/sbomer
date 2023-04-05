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
package org.jboss.sbomer.rest.errors;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.jboss.sbomer.core.errors.ErrorResponse;

import com.fasterxml.jackson.core.JsonParseException;

import lombok.extern.slf4j.Slf4j;

@Provider
@Slf4j
public class SbomerExceptionMapper implements ExceptionMapper<Throwable> {

    @Override
    public Response toResponse(Throwable ex) {
        int status = 500;
        String message = "An unexpected error occurred";
        List<String> errors = null;

        if (ex instanceof WebApplicationException) {
            WebApplicationException wex = (WebApplicationException) ex;

            if (wex.getCause() instanceof JsonParseException) {
                status = 400;
                message = "An error occurred while parsing the request";
                errors = Collections.singletonList(((JsonParseException) wex.getCause()).getOriginalMessage());
            }
        }

        ErrorResponse error = ErrorResponse.builder()
                .errorId(UUID.randomUUID().toString())
                .message(message)
                .errors(errors)
                .build();

        log.error(error.toString(), ex);

        return Response.status(status).entity(error).build();
    }

}
