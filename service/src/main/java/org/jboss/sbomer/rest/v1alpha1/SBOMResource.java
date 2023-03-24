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
package org.jboss.sbomer.rest.v1alpha1;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
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
import org.jboss.pnc.common.Strings;
import org.jboss.pnc.rest.api.parameters.PaginationParameters;
import org.jboss.sbomer.dto.response.Page;
import org.jboss.sbomer.model.Sbom;
import org.jboss.sbomer.service.SBOMService;
import org.jboss.sbomer.utils.enums.Generators;
import org.jboss.sbomer.utils.enums.Processors;
import org.jboss.sbomer.validation.exceptions.ValidationException;

@Path("/api/v1alpha1/sboms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
@Tag(name = "SBOMs", description = "Endpoints related to SBOM handling, version v1alpha1")
public class SBOMResource {

    @Inject
    SBOMService sbomService;

    /**
     * Make it possible to create a {@link Sbom} resource directly from the endpoint.
     *
     * TODO: Add authentication. It should be possible to create new SBOM's this way only from trusted places.
     *
     * @param sbom
     * @return
     */

    @POST
    @Operation(
            summary = "Save base SBOM",
            description = "Save submitted base SBOM. This endpoint expects a base SBOM in the CycloneDX format encapsulated in the BaseSBOM structure.")
    @Parameter(name = "sbom", description = "The SBOM to save")
    @APIResponses({
            @APIResponse(
                    responseCode = "201",
                    description = "The SBOM was successfully saved",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON)),
            @APIResponse(
                    responseCode = "400",
                    description = "Provided SBOM couldn't be saved, probably due to validation failures",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON)) })
    public Response create(final Sbom sbom) {
        try {
            sbomService.saveSbom(sbom);
            return Response.status(Status.CREATED).entity(sbom).build();
        } catch (ValidationException exc) {
            return Response.status(Status.BAD_REQUEST).entity(exc).build();
        }
    }

    @GET
    @Operation(summary = "List SBOMs", description = "List SBOMs available in the system, paginated.")
    @APIResponses({ @APIResponse(
            responseCode = "200",
            description = "List of SBOMs in the system for a particular page and size.",
            content = @Content(mediaType = MediaType.APPLICATION_JSON)) })
    public Page<Sbom> list(@Valid @BeanParam PaginationParameters paginationParams) {
        return sbomService.listSboms(paginationParams.getPageIndex(), paginationParams.getPageSize());
    }

    @GET
    @Path("{buildId}")
    @Operation(
            summary = "Get all SBOMs related to a PNC build.",
            description = "Get all the SBOMs related to the specified PNC build, paginated.")
    @Parameter(name = "buildId", description = "PNC build identifier", example = "ARYT3LBXDVYAC")
    @APIResponses({
            @APIResponse(
                    responseCode = "200",
                    description = "The SBOMs related to a specific PNC buildId.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON)),
            @APIResponse(
                    responseCode = "404",
                    description = "No SBOMs could be found for the specified PNC build.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON)) })
    public Page<Sbom> listAllWithPncBuildId(
            @PathParam("buildId") String buildId,
            @Valid @BeanParam PaginationParameters paginationParams) {
        return sbomService
                .listAllSbomsWithBuildId(buildId, paginationParams.getPageIndex(), paginationParams.getPageSize());
    }

    @POST
    @Operation(
            summary = "Generate a base SBOM based on the PNC build.",
            description = "SBOM base generation for a particular PNC build Id offloaded to the service.")
    @Parameter(name = "buildId", description = "PNC build identifier", example = "ARYT3LBXDVYAC")
    @Parameter(
            name = "generator",
            description = "Generator to use to generate the SBOM. If not specified, CycloneDX will be used. Options are `DOMINO`, `CYCLONEDX`",
            example = "CYCLONEDX")
    @Path("/generate/{buildId}")
    @APIResponses({ @APIResponse(
            responseCode = "201",
            description = "Schedules generation of a SBOM for a particular PNC buildId. This is an asynchronous call. It does execute the generation behind the scenes.",
            content = @Content(mediaType = MediaType.APPLICATION_JSON)) })
    public Response generate(@PathParam("buildId") String id, @QueryParam("generator") String generator)
            throws Exception {

        Generators gen = Generators.CYCLONEDX;
        if (!Strings.isEmpty(generator)) {
            try {
                gen = Generators.valueOf(generator);
            } catch (IllegalArgumentException iae) {
                return Response.status(Status.BAD_REQUEST)
                        .entity(
                                "The specified generator does not exist, allowed values are `CYCLONEDX` or `DOMINO`. Leave empty to use `CYCLONEDX`")
                        .build();
            }
        }

        return sbomService.generateSbomFromPncBuild(id, gen);
    }

    @POST
    @Operation(
            summary = "Save the base SBOM and run and enrichment.",
            description = "Save the base SBOM and run and enrichment..")
    @Parameter(name = "sbom", description = "The SBOM to save")
    @Parameter(
            name = "processor",
            description = "Processor to use to enrich the SBOM. If not specified, SBOM_PEDIGREE will be used. Options are `SBOM_PROPERTIES`, `SBOM_PEDIGREE`",
            example = "SBOM_PEDIGREE")
    @Path("/enrich")
    @APIResponses({
            @APIResponse(
                    responseCode = "202",
                    description = "The SBOM enrichment process was accepted.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON)),
            @APIResponse(
                    responseCode = "400",
                    description = "Provided SBOM couldn't be saved, probably due to validation failures",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON)) })
    public Response processEnrichmentOfBaseSbom(final Sbom sbom, @QueryParam("processor") String processor)
            throws Exception {

        Processors proc = Processors.SBOM_PEDIGREE;
        if (!Strings.isEmpty(processor)) {
            try {
                proc = Processors.valueOf(processor);
            } catch (IllegalArgumentException iae) {
                return Response.status(Status.BAD_REQUEST)
                        .entity(
                                "The specified processor does not exist, allowed values are `SBOM_PROPERTIES`, `SBOM_PEDIGREE`. Leave empty to use `SBOM_PEDIGREE`")
                        .build();
            }
        }

        sbomService.saveAndEnrichSbom(sbom, proc);
        return Response.status(Status.ACCEPTED).build();
    }

}
