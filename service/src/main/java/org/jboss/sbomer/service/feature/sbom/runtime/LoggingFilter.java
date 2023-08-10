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
package org.jboss.sbomer.service.feature.sbom.runtime;

import java.io.IOException;

import org.jboss.pnc.api.constants.MDCKeys;
import org.jboss.sbomer.core.utils.MDCUtils;
import org.slf4j.MDC;

import io.opentelemetry.api.trace.Span;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.Provider;
import lombok.extern.slf4j.Slf4j;

/**
 * Intercepts all requests and logs them
 *
 * @author Andrea Vibelli
 */
@Provider
@Slf4j
public class LoggingFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final String REQUEST_EXECUTION_START = "request-execution-start";

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        requestContext.setProperty(REQUEST_EXECUTION_START, System.currentTimeMillis());
        MDCUtils.setMDCFromRequestContext(requestContext);
        MDCUtils.addMDCFromOtelHeadersWithFallback(requestContext, Span.current().getSpanContext(), false);

        UriInfo uriInfo = requestContext.getUriInfo();
        Request request = requestContext.getRequest();

        String forwardedFor = requestContext.getHeaderString("X-FORWARDED-FOR");
        if (forwardedFor != null) {
            MDC.put(MDCKeys.X_FORWARDED_FOR_KEY, forwardedFor);
            log.info("Requested {} {}, forwardedFor: {}.", request.getMethod(), uriInfo.getRequestUri(), forwardedFor);
        } else {
            log.info("Requested {} {}.", request.getMethod(), uriInfo.getRequestUri());
        }
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext)
            throws IOException {

        Long startTime = (Long) requestContext.getProperty(REQUEST_EXECUTION_START);

        String took;
        if (startTime == null) {
            took = "-1";
        } else {
            took = Long.toString(System.currentTimeMillis() - startTime);
        }

        try (MDC.MDCCloseable mdcTook = MDC.putCloseable(MDCKeys.REQUEST_TOOK, took);
                MDC.MDCCloseable mdcStatus = MDC
                        .putCloseable(MDCKeys.RESPONSE_STATUS, Integer.toString(responseContext.getStatus()));) {
            log.info("Completed {}, took: {}ms.", requestContext.getUriInfo().getPath(), took);
        }
    }

}
