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
package org.redhat.sbomer.rest;

import java.util.List;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.validation.ConstraintViolation;
import javax.validation.Valid;
import javax.validation.Validator;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.pnc.rest.api.parameters.PaginationParameters;
import org.redhat.sbomer.dto.BaseSBOM;
import org.redhat.sbomer.dto.response.Page;
import org.redhat.sbomer.service.SBOMService;
import org.redhat.sbomer.validation.exceptions.ValidationException;

import lombok.extern.slf4j.Slf4j;

@Path("/sboms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
@Tag(name = "SBOMs", description = "Endpoints related to SBOM handling")
@Slf4j
public class SBOMResource {

    @Inject
    SBOMService sbomService;

    /**
     * Make it possible to create a {@link BaseSBOM} resource directly from the endpoint.
     *
     * TODO: We probably shouldn't be really exposing this endpoint, but it's convenient at development.
     *
     * @param sbom
     * @return
     */
    @POST
    @Operation(
            summary = "Create SBOM",
            description = "Save submitted SBOM. This endpoint expects an SBOM in the CycloneDX format serialized to JSON.")
    public Response take(final BaseSBOM sbom) {
        try {
            sbomService.saveBom(sbom);
            return Response.status(Status.CREATED).entity(sbom).build();
        } catch (ValidationException exc) {
            return Response.status(Status.BAD_REQUEST).entity(exc).build();
        }
    }

    @POST
    @Operation(
            summary = "Create SBOM based on the PNC build",
            description = "SBOM generation for a particular PNC build Id offloaded to the service")
    @Path("{id}")
    public Response fromBuild(@PathParam("id") String id) throws Exception {

        sbomService.createBomFromPncBuild(id);

        // Nothing is happening, yet!
        return Response.status(Status.ACCEPTED).build();
    }

    @GET
    @Operation(summary = "List of all SBOMs", description = "List all SBOMs available in the system")
    public Page<BaseSBOM> list(@Valid @BeanParam PaginationParameters paginationParams) {
        return sbomService.listBaseSboms(paginationParams.getPageIndex(), paginationParams.getPageSize());
    }

    @GET
    @Path("{buildId}")
    @Operation(summary = "Get specific BaseSBOM", description = "Get a specific BaseSBOM by the PNC buildId")
    public BaseSBOM get(@PathParam("buildId") String buildId) {
        return sbomService.getBaseSbom(buildId);
    }

}