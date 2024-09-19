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

import java.io.InputStream;

import org.cyclonedx.model.Bom;
import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.jboss.sbomer.core.errors.ClientException;
import org.jboss.sbomer.core.errors.ForbiddenException;
import org.jboss.sbomer.core.errors.NotFoundException;
import org.jboss.sbomer.core.errors.UnauthorizedException;

import io.quarkus.oidc.client.filter.OidcClientFilter;
import io.quarkus.rest.client.reactive.ClientExceptionMapper;
import io.smallrye.reactive.messaging.annotations.Blocking;
import jakarta.enterprise.context.ApplicationScoped;
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
@ApplicationScoped
@ClientHeaderParam(name = "User-Agent", value = "SBOMer")
@RegisterRestClient(configKey = "atlas")
@Path("/api/v1/sbom")
@OidcClientFilter
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface AtlasClient {

    @GET
    public Bom get(@QueryParam("id") String purl);

    // TODO: check why automatic serialization
    @PUT
    public void upload(@QueryParam("id") String purl, Bom bom);

    @PUT
    public void upload(@QueryParam("id") String purl, InputStream bom);

    @ClientExceptionMapper
    @Blocking
    static RuntimeException toException(Response response) {
        String message = response.readEntity(String.class);

        switch (response.getStatus()) {
            case 400:
                return new ClientException("Bad request, {}", message);
            case 401:
                return new UnauthorizedException("Caller is unauthorized to access resource; {}", message);
            case 403:
                return new ForbiddenException("Caller is forbidden to access resource; {}", message);
            case 404:
                return new NotFoundException("Requested resource was not found; {}", message);
            default:
                break;
        }

        return null;
    }
}
