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
package org.redhat.sbomer.rest.v1alpha1;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.pnc.rest.api.parameters.PaginationParameters;
import org.redhat.sbomer.dto.response.Page;
import org.redhat.sbomer.model.BaseSBOM;
import org.redhat.sbomer.service.SBOMService;
import org.redhat.sbomer.validation.exceptions.ValidationException;

import lombok.extern.slf4j.Slf4j;

@Path("/api/v1alpha1/sboms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
@Tag(name = "SBOMs", description = "Endpoints related to SBOM handling, version v1")
public class SBOMResource {

    @Inject
    SBOMService sbomService;

    /**
     * Make it possible to create a {@link BaseSBOM} resource directly from the endpoint.
     *
     * TODO: Add authentication. It should be possible to create new SBOM's this way only from trusted places.
     *
     * @param sbom
     * @return
     */
    @POST
    @Operation(
            summary = "Create SBOM",
            description = "Save submitted SBOM. This endpoint expects an SBOM in the CycloneDX format encapsulated in the BaseSBOM structure.")
    @APIResponses({
            @APIResponse(
                    responseCode = "201",
                    description = "The SBOM was successfully saved",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON)),
            @APIResponse(
                    responseCode = "400",
                    description = "Provided SBOM couldn't be saved, probably due to validation failures",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON)) })
    public Response create(final BaseSBOM sbom) {
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
    @Parameter(name = "buildId", description = "PNC build identifier", example = "ARYT3LBXDVYAC")
    @Path("{buildId}")
    @APIResponses({ @APIResponse(
            responseCode = "201",
            description = "Schedules generation of a SBOM for a particular PNC buildId. This is an asynchronous call. It does execute the generation behind the scenes.",
            content = @Content(mediaType = MediaType.APPLICATION_JSON)) })
    public Response fromBuild(@PathParam("buildId") String id) throws Exception {

        sbomService.createBomFromPncBuild(id);

        // Nothing is happening, yet!
        return Response.status(Status.ACCEPTED).build();
    }

    @GET
    @Operation(summary = "List SBOMs", description = "List SBOMs available in the system in a paginated way")
    @APIResponses({ @APIResponse(
            responseCode = "200",
            description = "List of SBOMs in the system for a particular page and size.",
            content = @Content(mediaType = MediaType.APPLICATION_JSON)) })
    public Page<BaseSBOM> list(@Valid @BeanParam PaginationParameters paginationParams) {
        return sbomService.listBaseSboms(paginationParams.getPageIndex(), paginationParams.getPageSize());
    }

    @GET
    @Path("{buildId}")
    @Operation(summary = "Get specific BaseSBOM", description = "Get a specific BaseSBOM by the PNC buildId")
    @Parameter(name = "buildId", description = "PNC build identifier", example = "ARYT3LBXDVYAC")
    @APIResponses({
            @APIResponse(
                    responseCode = "200",
                    description = "The BaseSBOM structure for a specific PNC buildId.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON)),
            @APIResponse(
                    responseCode = "404",
                    description = "The BaseSBOM for the particular buildID couldn't be found in the system.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON)) })
    public BaseSBOM get(@PathParam("buildId") String buildId) {
        return sbomService.getBaseSbom(buildId);
    }

    @POST
    @Operation(
            summary = "Enrich SBOM based on the PNC build",
            description = "SBOM enrichment for a particular PNC build Id. Only sbom-spec currently available is `properties`")
    @Parameter(name = "buildId", description = "PNC build identifier", example = "ARYT3LBXDVYAC")
    @Path("/enrich/{buildId}")
    @APIResponses({ @APIResponse(
            responseCode = "201",
            description = "Executes the enrichment of an existing SBOM for a particular PNC buildId.",
            content = @Content(mediaType = MediaType.APPLICATION_JSON)) })
    public Response runEnrichmentOfBaseSbom(
            @PathParam("buildId") String buildId,
            @QueryParam("sbomSpec") String sbomSpec) throws Exception {

        try {
            BaseSBOM enrichedSBOM = sbomService.runEnrichmentOfBaseSbom(buildId, sbomSpec);
            return Response.status(Response.Status.OK).entity(enrichedSBOM).build();
        } catch (NotFoundException nfe) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("No existing baseSBOM for buildId: " + buildId)
                    .build();
        } catch (ValidationException vExc) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(vExc.getMessage()).build();
        }
    }

}