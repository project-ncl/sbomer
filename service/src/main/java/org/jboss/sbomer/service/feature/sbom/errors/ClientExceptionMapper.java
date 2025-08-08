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

import java.util.Map;

import org.jboss.sbomer.core.errors.ClientException;
import org.jboss.sbomer.core.errors.ErrorResponse;
import org.jboss.sbomer.core.features.sbom.utils.OtelHelper;
import org.slf4j.MDC;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.ext.Provider;
import lombok.extern.slf4j.Slf4j;

@Provider
@Slf4j
public class ClientExceptionMapper extends AbstractExceptionMapper<ClientException> {

    @Override
    public Response toResponse(ClientException ex) {
        ErrorResponse error = ErrorResponse.builder()
                .resource(uriInfo.getPath())
                .errorId(ex.getErrorId())
                .error(Status.fromStatusCode(ex.getCode()).getReasonPhrase())
                .message(ex.getMessage())
                .errors(ex.getErrors())
                .build();

        OtelHelper.withSpan(this.getClass(), ".toResponse", Map.of(), MDC.getCopyOfContextMap(), () -> {
            log.error(error.toString(), ex);
            Span span = Span.current();
            span.recordException(ex);
            span.setStatus(StatusCode.ERROR, ex.getMessage());
            return null;
        });

        return Response.status(ex.getCode()).entity(error).type(MediaType.APPLICATION_JSON).build();
    }
}
