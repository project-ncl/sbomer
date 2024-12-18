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
package org.jboss.sbomer.service.feature.sbom.atlas;

import java.util.List;

import org.jboss.sbomer.core.patch.cyclonedx.model.Bom;
import org.jboss.sbomer.core.errors.ClientException;
import org.jboss.sbomer.core.errors.ForbiddenException;
import org.jboss.sbomer.core.errors.NotFoundException;
import org.jboss.sbomer.core.errors.UnauthorizedException;

import com.fasterxml.jackson.databind.JsonNode;

import io.quarkus.rest.client.reactive.ClientExceptionMapper;
import io.smallrye.reactive.messaging.annotations.Blocking;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * A client for the Atlas (instance of the Trusted Profile Analyzer).
 */

@Path("/api/v1/sbom")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface AtlasClient {

    @GET
    public Bom get(@QueryParam("id") String purl);

    @PUT
    public void upload(@QueryParam("id") String purl, JsonNode bom);

    @ClientExceptionMapper
    @Blocking
    static RuntimeException toException(Response response) {
        String message = response.readEntity(String.class);

        switch (response.getStatus()) {
            case 400:
                return new ClientException("Bad request", List.of(message));
            case 401:
                return new UnauthorizedException(
                        "Caller is unauthorized to access resource; {}",
                        message,
                        List.of(message));
            case 403:
                return new ForbiddenException("Caller is forbidden to access resource; {}", message, List.of(message));
            case 404:
                return new NotFoundException("Requested resource was not found; {}", message, List.of(message));
            default:
                break;
        }

        return null;
    }
}
