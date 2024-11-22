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
package org.jboss.sbomer.service.rest;

import static org.jboss.sbomer.service.feature.sbom.model.RequestEvent.EVENT_KEY_REST_METHOD;
import static org.jboss.sbomer.service.feature.sbom.model.RequestEvent.EVENT_KEY_REST_ADDRESS;
import static org.jboss.sbomer.service.feature.sbom.model.RequestEvent.EVENT_KEY_REST_USERNAME;
import static org.jboss.sbomer.service.feature.sbom.model.RequestEvent.EVENT_KEY_REST_TRACE_ID;
import static org.jboss.sbomer.service.feature.sbom.model.RequestEvent.EVENT_KEY_REST_SPAN_ID;
import static org.jboss.sbomer.service.feature.sbom.model.RequestEvent.EVENT_KEY_REST_URI_PATH;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;
import jakarta.validation.ConstraintViolation;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.core.UriInfo;
import lombok.extern.slf4j.Slf4j;

import org.hibernate.validator.engine.HibernateConstraintViolation;
import org.jboss.sbomer.core.config.request.RequestConfig;
import org.jboss.sbomer.service.feature.sbom.model.RequestEvent;
import org.jboss.sbomer.service.feature.sbom.model.RequestEventType;
import org.yaml.snakeyaml.parser.ParserException;

import io.opentelemetry.api.trace.Span;

@Slf4j
public class RestUtils {
    private RestUtils() {
        // This is a utility class
    }

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

    /**
     * Gets the user principal (if available) from the SecurityContex of a ContainerRequestContext.
     *
     * @param context the container request context
     * @return user principal name
     */
    public static String getUserPrincipalName(ContainerRequestContext context) {
        SecurityContext securityContext = context.getSecurityContext();
        if (securityContext != null) {
            Principal userPrincipal = securityContext.getUserPrincipal();
            if (userPrincipal != null) {
                return userPrincipal.getName();
            }
        }
        return "<none>";
    }

    /**
     * Creates a request from a REST API call event.
     *
     * @param requestConfig the configuration posted to the REST API call
     * @param requestContext the container request context
     * @return the request object
     */
    @Transactional(value = TxType.REQUIRES_NEW)
    public static RequestEvent createRequestFromRestEvent(
            RequestConfig requestConfig,
            ContainerRequestContext requestContext,
            Span currentSpan) {

        UriInfo uriInfo = requestContext.getUriInfo();
        String clientIp = Optional.ofNullable(requestContext.getHeaderString("X-Forwarded-For")).orElse("<none>");
        String method = requestContext.getMethod().toUpperCase();
        String userPrincipal = getUserPrincipalName(requestContext);
        String traceId = currentSpan.getSpanContext().getTraceId();
        String spanId = currentSpan.getSpanContext().getSpanId();

        Map<String, String> event = Map.of(
                EVENT_KEY_REST_METHOD,
                method,
                EVENT_KEY_REST_URI_PATH,
                uriInfo.getRequestUri().toString(),
                EVENT_KEY_REST_ADDRESS,
                clientIp,
                EVENT_KEY_REST_USERNAME,
                userPrincipal,
                EVENT_KEY_REST_TRACE_ID,
                traceId,
                EVENT_KEY_REST_SPAN_ID,
                spanId);

        return RequestEvent.createNew(requestConfig, RequestEventType.REST, event).save();
    }
}
