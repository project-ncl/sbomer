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
package org.jboss.sbomer.cli.feature.sbom.client;

import javax.enterprise.context.ApplicationScoped;
import javax.validation.Valid;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.jboss.pnc.rest.api.parameters.PaginationParameters;
import org.jboss.sbomer.cli.feature.sbom.model.Sbom;
import org.jboss.sbomer.cli.feature.sbom.model.SbomGenerationRequest;

/**
 * Client used to interact with the SBOMer REST API.
 */
@ApplicationScoped
@RegisterRestClient(configKey = "sbomer")
@Path("/api/v1alpha1/sboms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface SBOMerClient {

    /**
     * Retrieves SBOM based on the ID.
     *
     * @param id
     * @return {@link Sbom}
     */
    @GET
    @Path("/{id}")
    Response getById(@HeaderParam("log-process-context") String processContext, @PathParam("id") String id);

    /**
     * Retrieves SBOM Generation Request based on the ID.
     *
     * @param id
     * @return {@link SbomGenerationRequest}
     */
    @GET
    @Path("/requests/{id}")
    Response getGenerationRequestById(
            @HeaderParam("log-process-context") String processContext,
            @PathParam("id") String id);

    /**
     * Search the base SBOM based on the build ID via RSQL search and pagination.
     *
     * @param paginationParams
     * @param rsqlQuery
     * @return {@link Response}
     */
    @GET
    Response searchSboms(
            @HeaderParam("log-process-context") String processContext,
            @Valid @BeanParam PaginationParameters paginationParams,
            @QueryParam("query") String rsqlQuery,
            @QueryParam("sort") String rsqlSort);

    @GET
    @Path("/requests")
    Response searchGenerationRequests(
            @HeaderParam("log-process-context") String processContext,
            @Valid @BeanParam PaginationParameters paginationParams,
            @QueryParam("query") String rsqlQuery,
            @QueryParam("sort") String rsqlSort);
}