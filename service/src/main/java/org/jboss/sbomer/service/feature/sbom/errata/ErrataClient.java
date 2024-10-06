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
package org.jboss.sbomer.service.feature.sbom.errata;

import java.util.List;

import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.jboss.sbomer.core.errors.ClientException;
import org.jboss.sbomer.core.errors.ForbiddenException;
import org.jboss.sbomer.core.errors.NotFoundException;
import org.jboss.sbomer.core.errors.UnauthorizedException;
import org.jboss.sbomer.service.feature.sbom.errata.dto.Errata;
import org.jboss.sbomer.service.feature.sbom.errata.dto.ErrataProduct;
import org.jboss.sbomer.service.feature.sbom.errata.dto.ErrataRelease;
import org.jboss.sbomer.service.feature.sbom.errata.dto.ErrataVariant;

import io.quarkiverse.kerberos.client.KerberosClientRequestFilter;
import io.quarkus.rest.client.reactive.ClientExceptionMapper;
import io.smallrye.reactive.messaging.annotations.Blocking;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * A client for Errata
 */
@ApplicationScoped
@ClientHeaderParam(name = "User-Agent", value = "SBOMer")
@RegisterRestClient(configKey = "errata")
@Path("/api/v1")
@RegisterProvider(KerberosClientRequestFilter.class)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface ErrataClient {

    // Retrieve the advisory data, the id could be advisory id or advisory name.
    @GET
    @Path("/erratum/{id}")
    public Errata getErratum(@QueryParam("id") String errataId);

    // Get the details of a product by its id or short name
    @GET
    @Path("/products/{id}")
    public ErrataProduct getProduct(@QueryParam("id") String productId);

    // Get the details of a release by its id or name
    @GET
    @Path("/releases/{id}")
    public ErrataRelease getReleases(@QueryParam("id") String releaseId);

    // Get the details of a variant by its name or id
    @GET
    @Path("/variants/{id}")
    public ErrataVariant getVariants(@QueryParam("id") String variantId);

    // Get Brew build details.
    @GET
    @Path("/build/{id_or_nvr}")
    // TODO DTOs
    public String getBrewBuildDetails(@QueryParam("id_or_nvr") String brewIdOrNvr);

    // Fetch the Brew builds associated with an advisory.
    @GET
    @Path("/erratum/{id}/builds")
    // TODO DTOs
    public String getBuilds(@QueryParam("id") String errataId);

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
