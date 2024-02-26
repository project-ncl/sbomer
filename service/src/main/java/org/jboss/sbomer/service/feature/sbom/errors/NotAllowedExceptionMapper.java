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

import jakarta.ws.rs.NotAllowedException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.ResponseBuilder;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.ext.Provider;
import lombok.extern.slf4j.Slf4j;

@Provider
@Slf4j
public class NotAllowedExceptionMapper extends AbstractExceptionMapper<NotAllowedException> {

    @Override
    Status getStatus() {
        return Status.METHOD_NOT_ALLOWED;
    }

    @Override
    Response hook(ResponseBuilder responseBuilder, Throwable ex) {
        log.warn(
                "Client requested '{}' resource with not allowed method: '{}'",
                uriInfo.getPath(),
                request.getMethod(),
                ex);
        return responseBuilder.build();
    }

    @Override
    String errorMessage(NotAllowedException ex) {
        return formattedString(
                "Requesting resource '{}' using '{}' method is not allowed. Please consult API documentation.",
                uriInfo.getPath(),
                request.getMethod());
    }
}
