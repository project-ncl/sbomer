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

import java.util.List;
import java.util.UUID;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.NotAllowedException;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.Map;
import org.jboss.resteasy.spi.Failure;
import org.jboss.sbomer.core.errors.ErrorResponse;

import lombok.extern.slf4j.Slf4j;

@Provider
@Slf4j
public class DefaultExceptionMapper implements ExceptionMapper<Exception> {

    @Override
    public Response toResponse(Exception e) {

        List<String> errors = null;
        int status = Response.Status.INTERNAL_SERVER_ERROR.getStatusCode();
        Response response = null;

        if (e instanceof WebApplicationException) {
            response = ((WebApplicationException) e).getResponse();
            if (e instanceof NotFoundException) {
                log.info("Resource requested by a client was not found.", e);
                return response; // In case of 404 we want to return the empty body.
            } else if (e instanceof ForbiddenException) {
                log.warn("Access to a resource requested by a client has been forbidden.", e);
            } else if (e instanceof NotAllowedException) {
                log.warn("Client requesting a resource method that is not allowed.", e);
            } else if (e instanceof NotAuthorizedException) {
                log.warn("Request authorization failure.", e);
            } else {
                log.warn("WebApplicationException occurred when processing REST response", e);
            }
        } else if (e instanceof Failure) { // Resteasy support
            Failure failure = ((Failure) e);
            if (failure.getErrorCode() > 0) {
                status = failure.getErrorCode();
            }
            response = failure.getResponse();
            log.warn("Failure occurred when processing REST response", e);
        } else {
            log.error("An exception occurred when processing REST response", e);
        }

        Response.ResponseBuilder builder;

        if (response != null) {
            builder = Response.status(response.getStatus());

            // copy headers
            for (Map.Entry<String, List<Object>> en : response.getMetadata().entrySet()) {
                String headerName = en.getKey();
                List<Object> headerValues = en.getValue();
                for (Object headerValue : headerValues) {
                    builder.header(headerName, headerValue);
                }
            }
        } else {
            builder = Response.status(status);
        }

        ErrorResponse error = ErrorResponse.builder()
                .errorId(UUID.randomUUID().toString())
                .errorType(e.getClass().getSimpleName())
                .message(e.getMessage())
                .errors(errors)
                .build();

        return builder.entity(error).type(MediaType.APPLICATION_JSON).build();
    }

}
