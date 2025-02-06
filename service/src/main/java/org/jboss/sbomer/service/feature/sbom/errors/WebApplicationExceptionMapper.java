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

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.ResponseBuilder;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.ext.Provider;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Provider
public class WebApplicationExceptionMapper extends AbstractExceptionMapper<WebApplicationException> {
    @Override
    Status getStatus(WebApplicationException ex) {
        return ex.getResponse().getStatusInfo().toEnum();
    }

    @Override
    Response hook(ResponseBuilder responseBuilder, WebApplicationException ex) {
        log.warn("Web application error occurred, unable to process request", ex);
        return responseBuilder.build();
    }

    @Override
    String errorMessage(WebApplicationException ex) {
        return formattedString("Unable to process request: {}", ex.getMessage());
    }

}
