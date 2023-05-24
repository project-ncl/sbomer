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
import org.eclipse.microprofile.openapi.annotations.media.ExampleObject;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.pnc.common.Strings;
import org.jboss.pnc.rest.api.parameters.PaginationParameters;
import org.jboss.sbomer.core.enums.GeneratorImplementation;
import org.jboss.sbomer.core.errors.ClientException;
import org.jboss.sbomer.core.errors.NotFoundException;
import org.jboss.sbomer.core.service.rest.Page;
import org.jboss.sbomer.core.utils.MDCUtils;
import org.jboss.sbomer.core.utils.SbomUtils;
import org.jboss.sbomer.model.Sbom;
import org.jboss.sbomer.service.GenerationService;
import org.jboss.sbomer.service.ProcessingService;
import org.jboss.sbomer.service.SbomService;

import com.fasterxml.jackson.databind.JsonNode;

import cz.jirutka.rsql.parser.RSQLParserException;

@Path("/api/v1alpha1/sboms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
@Tag(name = "SBOMs", description = "Endpoints related to SBOM handling, version v1alpha1, with RSQL capabilities")
public class SBOMResource {

    @Inject
    SbomService sbomService;

    @Inject
    GenerationService generationService;

    @Inject
    ProcessingService processingService;

    @GET
    @Operation(summary = "List SBOMs", description = "List paginated SBOMs using RSQL advanced search.")
    @Parameter(
            name = "query",
            description = "A RSQL query to search the SBOMs",
            examples = {
                    @ExampleObject(name = "Find all SBOMs with provided buildId", value = "buildId=eq=ABCDEFGHIJKLM"),
                    @ExampleObject(
                            name = "Find all SBOMs with provided purl",
                            value = "rootPurl=eq='pkg:maven/com.github.michalszynkiewicz.test/empty@1.0.0.redhat-00270?type=jar'") })
    @APIResponses({
            @APIResponse(
                    responseCode = "200",
                    description = "List of SBOMs in the system for a specified RSQL query.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON)),
            @APIResponse(
                    responseCode = "400",
                    description = "Failed while parsing the provided RSQL string, please verify the correct syntax.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON)),
            @APIResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON)) })
    public Response searchSboms(
            @Valid @BeanParam PaginationParameters paginationParams,
            @QueryParam("query") String rsqlQuery) {

        try {
            Page<Sbom> sboms = sbomService
                    .searchByQueryPaginated(paginationParams.getPageIndex(), paginationParams.getPageSize(), rsqlQuery);
            return Response.status(Status.OK).entity(sboms).build();
        } catch (IllegalArgumentException iae) {
            return Response.status(Status.BAD_REQUEST).entity(iae.getMessage()).build();
        } catch (RSQLParserException rsqlExc) {
            return Response.status(Status.BAD_REQUEST)
                    .entity("Failed while parsing the provided RSQL string, please verify the correct syntax")
                    .build();
        }
    }

    @GET
    @Path("{id}")
    @Operation(summary = "Get specific SBOM", description = "Get specific SBOM with the provided ID.")
    @Parameter(name = "id", description = "SBOM identifier", example = "429305915731435500")
    @APIResponses({
            @APIResponse(
                    responseCode = "200",
                    description = "The SBOM",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON)),
            @APIResponse(
                    responseCode = "400",
                    description = "Could not parse provided arguments",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON)),
            @APIResponse(
                    responseCode = "404",
                    description = "Requested SBOM could not be found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON)),
            @APIResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON)), })
    public Response getById(@PathParam("id") String sbomId) {
        Sbom sbom = doGetBomById(sbomId);
        return Response.status(Status.OK).entity(sbom).build();
    }

    @GET
    @Path("{id}/bom")
    @Operation(
            summary = "Get the BOM content of particular SBOM",
            description = "Get the BOM content of particular SBOM")
    @Parameter(name = "id", description = "SBOM identifier", example = "429305915731435500")
    @APIResponses({
            @APIResponse(
                    responseCode = "200",
                    description = "The BOM",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON)),
            @APIResponse(
                    responseCode = "400",
                    description = "Could not parse provided arguments",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON)),
            @APIResponse(
                    responseCode = "404",
                    description = "Requested SBOM could not be found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON)),
            @APIResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON)), })
    public Response getBomById(@PathParam("id") String sbomId) {
        Sbom sbom = doGetBomById(sbomId);
        return Response.status(Status.OK).entity(SbomUtils.toJsonNode(sbom.getCycloneDxBom())).build();
    }

    /**
     * Update the Bom within the {@link Sbom} resource.
     *
     * @param sbom {@link Sbom}
     * @return
     */
    @POST
    @Operation(
            summary = "Update Bom for specified SBOM",
            description = "Save submitted SBOM. This endpoint expects a SBOM in the CycloneDX format encapsulated in the structure.")
    @Parameter(name = "sbom", description = "The SBOM to save")
    @APIResponses({
            @APIResponse(
                    responseCode = "200",
                    description = "The SBOM was successfully saved",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON)),
            @APIResponse(
                    responseCode = "400",
                    description = "Could not parse provided arguments",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON)),
            @APIResponse(
                    responseCode = "422",
                    description = "Provided SBOM couldn't be saved because of validation failures",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON)),
            @APIResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON)), })
    @Path("{id}/bom")
    public Response updateSbom(@PathParam("id") String sbomId, final JsonNode bom) {
        Long id = null;

        try {
            id = Long.valueOf(sbomId);
        } catch (NumberFormatException e) {
            throw new ClientException(400, "Invalid SBOM id provided: '{}', a number was expected", sbomId);
        }

        Sbom sbom = sbomService.updateBom(id, bom);
        return Response.status(Status.OK).entity(sbom).build();
    }

    private Sbom doGetBomById(String sbomId) {
        Sbom sbom = sbomService.get(sbomId);

        if (sbom == null) {
            throw new NotFoundException("SBOM with id '{}' not found", sbomId);
        }

        return sbom;
    }

    // Deprecated endpoints:
    // -------------------------------------------------------------------------------
    // @GET
    // @Path("/build/{buildId}") --> OLD
    // @Operation(summary = "Get all SBOMs related to a PNC build")

    // ==> Use "/api/v1alpha1/sboms?query=buildId==eq={buildId}"
    // -------------------------------------------------------------------------------
    // @GET
    // @Path("/build/{buildId}")
    // @Operation(summary = "Get the base SBOM related to a PNC build")

    // ==> Use "/api/v1alpha1/sboms?query=buildId=eq={buildId};generator=isnull=false;processors=isnull=true"
    // -------------------------------------------------------------------------------
    // @GET
    // @Path("/build/{buildId}/enriched")
    // @Operation(summary = "Get the enriched SBOM related to a PNC build")

    // ==> Use "/api/v1alpha1/sboms?query=buildId=eq={buildId};generator=isnull=false;processors=isnull=false"
    // -------------------------------------------------------------------------------
    // @GET
    // @Path("/purl/{rootPurl}/base")
    // @Operation(summary = "Get the base SBOM related to a root component purl")

    // ==> Use "/api/v1alpha1/sboms?query=rootPurl=eq='{rootPurl}';generator=isnull=false;processors=isnull=true"
    // -------------------------------------------------------------------------------
    // @GET
    // @Path("/purl/{rootPurl}/enriched")
    // @Operation(summary = "Get the enriched SBOM related to a root component purl")

    // ==> Use "/api/v1alpha1/sboms?query=rootPurl=eq='{rootPurl}';generator=isnull=false;processors=isnull=false"
    // -------------------------------------------------------------------------------
    // @GET
    // @Path("/build/{buildId}/base/bom")
    // @Operation(summary = "Get the base SBOM content related to a PNC build")
    // -------------------------------------------------------------------------------
    // @GET
    // @Path("/build/{buildId}/enriched/bom")
    // @Operation(summary = "Get the enriched SBOM content related to a PNC build")
    // -------------------------------------------------------------------------------
    // @GET
    // @Path("/purl/{rootPurl}/base/bom")
    // @Operation(summary = "Get the base SBOM content related to a root component purl")
    // -------------------------------------------------------------------------------
    // @GET
    // @Path("/purl/{rootPurl}/enriched/bom")
    // @Operation(summary = "Get the enriched SBOM content related to a root component purl")
    // -------------------------------------------------------------------------------

    @POST
    @Operation(
            summary = "Generate a base SBOM based on the PNC build",
            description = "SBOM base generation for a particular PNC build Id offloaded to the service.")
    @Parameter(name = "id", description = "PNC build identifier", example = "ARYT3LBXDVYAC")
    @Parameter(
            name = "generator",
            description = "Generator to use to generate the SBOM. If not specified, CycloneDX will be used. Options are `DOMINO`, `CYCLONEDX`",
            example = "CYCLONEDX")
    @Path("/generate/build/{buildId}")
    @APIResponses({ @APIResponse(
            responseCode = "202",
            description = "Schedules generation of a SBOM for a particular PNC buildId. This is an asynchronous call. It does execute the generation behind the scenes.",
            content = @Content(mediaType = MediaType.APPLICATION_JSON)),
            @APIResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON)) })
    public Response generate(@PathParam("buildId") String buildId, @QueryParam("generator") String generator)
            throws Exception {

        try {
            MDCUtils.addBuildContext(buildId);

            GeneratorImplementation gen = GeneratorImplementation.CYCLONEDX;

            if (!Strings.isEmpty(generator)) {
                try {
                    gen = GeneratorImplementation.valueOf(generator);
                } catch (IllegalArgumentException iae) {
                    throw new ClientException(
                            Status.BAD_REQUEST.getStatusCode(),
                            "The specified generator does not exist, allowed values are `CYCLONEDX` or `DOMINO`. Leave empty to use `CYCLONEDX`",
                            iae);
                }
            }

            Sbom sbom = generationService.generate(buildId, gen);

            return Response.status(Status.ACCEPTED).entity(sbom).build();
        } finally {
            MDCUtils.removeBuildContext();
        }
    }

    @POST
    @Operation(summary = "Process selected SBOM", description = "Process selected SBOM using default processors")
    @Parameter(name = "id", description = "The SBOM identifier")
    @Path("/{id}/process")
    @APIResponses({
            @APIResponse(
                    responseCode = "202",
                    description = "The SBOM enrichment process was accepted.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON)),
            @APIResponse(
                    responseCode = "400",
                    description = "Could not parse provided arguments",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON)),
            @APIResponse(
                    responseCode = "404",
                    description = "Requested SBOM could not be found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON)),
            @APIResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON)), })
    public Response processEnrichmentOfBaseSbom(@PathParam("id") final String sbomId) throws Exception {

        try {
            Sbom sbom = doGetBomById(sbomId);
            MDCUtils.addBuildContext(sbom.getBuildId());
            sbom = processingService.process(sbom);

            return Response.status(Status.ACCEPTED).entity(sbom).build();
        } finally {
            MDCUtils.removeBuildContext();
        }
    }

}