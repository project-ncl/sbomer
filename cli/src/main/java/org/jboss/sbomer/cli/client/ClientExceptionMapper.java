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
package org.jboss.sbomer.cli.client;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import javax.inject.Inject;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.ext.ResponseExceptionMapper;
import org.jboss.sbomer.core.errors.ApiException;
import org.jboss.sbomer.core.errors.ApplicationException;
import org.jboss.sbomer.core.errors.ErrorResponse;

import com.fasterxml.jackson.databind.ObjectMapper;

public class ClientExceptionMapper implements ResponseExceptionMapper<Throwable> {

    @Inject
    ObjectMapper objectMapper;

    @Override
    public ApiException toThrowable(Response response) {

        ErrorResponse error = null;

        try {
            error = objectMapper.readValue((ByteArrayInputStream) response.getEntity(), ErrorResponse.class);
        } catch (IOException e) {
            throw new ApplicationException(
                    "An error occurred while parsing the error from the service, please contact developers",
                    e);
        }

        return ApiException.fromResponse(response.getStatus(), error);
    }

}
