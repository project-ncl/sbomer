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

import org.jboss.resteasy.spi.Failure;
import org.jboss.sbomer.core.errors.ErrorResponse;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.ResponseBuilder;
import jakarta.ws.rs.core.Response.StatusType;
import jakarta.ws.rs.ext.Provider;
import lombok.extern.slf4j.Slf4j;

@Provider
@Slf4j
public class ResteasyFailureMapper extends AbstractExceptionMapper<Failure> {

    @Override
    ErrorResponse errorResponse(Failure ex) {
        Failure failure = ((Failure) ex);
        StatusType statusType = failure.getResponse().getStatusInfo();

        return ErrorResponse.builder()
                .resource(uriInfo.getPath())
                .errorId(generateErrorId())
                .error(statusType.getReasonPhrase())
                .message(errorMessage(ex))
                .build();
    }

    @Override
    Response hook(ResponseBuilder responseBuilder, Throwable ex) {
        log.error("Failure occurred while processing request", ex);

        Failure failure = ((Failure) ex);

        if (failure.getErrorCode() > 0) {
            // Update the status based on the one defined in the Failure object
            responseBuilder.status(failure.getErrorCode());
        }

        return responseBuilder.build();
    }

}
