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
package org.jboss.sbomer.service.feature.sbom.pyxis;

import static org.jboss.sbomer.core.rest.faulttolerance.Constants.PYXIS_CLIENT_DELAY;
import static org.jboss.sbomer.core.rest.faulttolerance.Constants.PYXIS_CLIENT_MAX_RETRIES;

import java.time.temporal.ChronoUnit;
import java.util.List;

import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.jboss.sbomer.core.errors.ClientException;
import org.jboss.sbomer.core.errors.ForbiddenException;
import org.jboss.sbomer.core.errors.NotFoundException;
import org.jboss.sbomer.core.errors.UnauthorizedException;
import org.jboss.sbomer.core.rest.faulttolerance.RetryLogger;
import org.jboss.sbomer.service.feature.sbom.kerberos.PyxisKrb5ClientRequestFilter;
import org.jboss.sbomer.service.feature.sbom.pyxis.dto.PyxisRepository;
import org.jboss.sbomer.service.feature.sbom.pyxis.dto.PyxisRepositoryDetails;

import io.quarkus.rest.client.reactive.ClientExceptionMapper;
import io.smallrye.faulttolerance.api.BeforeRetry;
import io.smallrye.faulttolerance.api.ExponentialBackoff;
import io.smallrye.reactive.messaging.annotations.Blocking;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * A client for Pyxis
 */
@ApplicationScoped
@ClientHeaderParam(name = "User-Agent", value = "SBOMer")
@RegisterRestClient(configKey = "pyxis")
@Path("/v1")
@RegisterProvider(PyxisKrb5ClientRequestFilter.class)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface PyxisClient {

    List<String> REPOSITORIES_DETAILS_INCLUDES = List.of(
            "data.repositories.registry",
            "data.repositories.repository",
            "data.repositories.tags",
            "data.repositories.published");
    List<String> REPOSITORIES_REGISTRY_INCLUDES = List.of("_id", "registry", "repository", "requires_terms");

    @GET
    @Path("/images/nvr/{nvr}")
    @Retry(
            maxRetries = PYXIS_CLIENT_MAX_RETRIES,
            delay = PYXIS_CLIENT_DELAY,
            delayUnit = ChronoUnit.SECONDS,
            abortOn = UnauthorizedException.class)
    @ExponentialBackoff
    @BeforeRetry(RetryLogger.class)
    PyxisRepositoryDetails getRepositoriesDetails(
            @PathParam("nvr") String nvr,
            @QueryParam("include") List<String> includes);

    @GET
    @Path("/repositories/registry/{registry}/repository/{repository}")
    @Retry(
            maxRetries = PYXIS_CLIENT_MAX_RETRIES,
            delay = PYXIS_CLIENT_DELAY,
            delayUnit = ChronoUnit.SECONDS,
            abortOn = UnauthorizedException.class)
    @ExponentialBackoff
    @BeforeRetry(RetryLogger.class)
    PyxisRepository getRepository(
            @PathParam("registry") String registry,
            @PathParam("repository") String repository,
            @QueryParam("include") List<String> includes);

    @ClientExceptionMapper
    @Blocking
    static RuntimeException toException(Response response) {
        String message = response.readEntity(String.class);

        return switch (response.getStatus()) {
            case 400 -> new ClientException("Bad request", List.of(message));
            case 401 ->
                new UnauthorizedException("Caller is unauthorized to access resource; {}", message, List.of(message));
            case 403 -> new ForbiddenException("Caller is forbidden to access resource; {}", message, List.of(message));
            case 404 -> new NotFoundException("Requested resource was not found; {}", message, List.of(message));
            default -> null;
        };

    }

}
