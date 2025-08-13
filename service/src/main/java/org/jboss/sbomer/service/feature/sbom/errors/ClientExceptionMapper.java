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

import static org.jboss.sbomer.core.features.sbom.utils.MDCUtils.MDC_SPAN_ID_KEY;
import static org.jboss.sbomer.core.features.sbom.utils.MDCUtils.MDC_TRACE_FLAGS_KEY;
import static org.jboss.sbomer.core.features.sbom.utils.MDCUtils.MDC_TRACE_ID_KEY;
import static org.jboss.sbomer.core.features.sbom.utils.MDCUtils.MDC_TRACE_STATE_KEY;

import java.util.Map;

import org.jboss.pnc.common.otel.OtelUtils;
import org.jboss.sbomer.core.errors.ClientException;
import org.jboss.sbomer.core.errors.ErrorResponse;
import org.jboss.sbomer.core.features.sbom.utils.OtelHelper;
import org.slf4j.MDC;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.context.Scope;
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
        Span span = OtelUtils
                .buildChildSpan(
                        GlobalOpenTelemetry.get().getTracer(""),
                        OtelHelper.getEffectiveClassName(this.getClass()) + ".toResponse",
                        SpanKind.CLIENT,
                        MDC.get(MDC_TRACE_ID_KEY),
                        MDC.get(MDC_SPAN_ID_KEY),
                        MDC.get(MDC_TRACE_FLAGS_KEY),
                        MDC.get(MDC_TRACE_STATE_KEY),
                        Span.current().getSpanContext(),
                        Map.of())
                .startSpan();
        try (Scope scope = span.makeCurrent()) {
            span.recordException(ex);
            span.setStatus(StatusCode.ERROR, ex.getMessage());

            ErrorResponse error = ErrorResponse.builder()
                    .resource(uriInfo.getPath())
                    .errorId(ex.getErrorId())
                    .error(Status.fromStatusCode(ex.getCode()).getReasonPhrase())
                    .message(ex.getMessage())
                    .errors(ex.getErrors())
                    .build();

            log.error(error.toString(), ex);

            return Response.status(ex.getCode()).entity(error).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable t) {
            span.recordException(t);
            span.setStatus(StatusCode.ERROR, t.getMessage());
            throw t;
        } finally {
            span.end();
        }
    }
}
