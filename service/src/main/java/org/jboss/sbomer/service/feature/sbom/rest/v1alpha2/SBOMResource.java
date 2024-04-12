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
package org.jboss.sbomer.service.feature.sbom.rest.v1alpha2;

import java.util.Map;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.ExampleObject;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.sbomer.core.dto.v1alpha2.SbomGenerationRequestRecord;
import org.jboss.sbomer.core.dto.v1alpha2.SbomRecord;
import org.jboss.sbomer.core.dto.v1alpha2.BaseSbomRecord;
import org.jboss.sbomer.core.errors.ErrorResponse;
import org.jboss.sbomer.core.features.sbom.config.runtime.Config;
import org.jboss.sbomer.core.features.sbom.rest.Page;
import org.jboss.sbomer.core.utils.PaginationParameters;
import org.jboss.sbomer.service.feature.sbom.features.FeatureFlags;
import org.jboss.sbomer.service.feature.sbom.mapper.V1Alpha2Mapper;
import org.jboss.sbomer.service.feature.sbom.model.Sbom;
import org.jboss.sbomer.service.feature.sbom.model.SbomGenerationRequest;
import org.jboss.sbomer.service.feature.sbom.rest.api.AbstractApiProvider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.jakarta.rs.yaml.YAMLMediaTypes;

import jakarta.annotation.security.PermitAll;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import lombok.extern.slf4j.Slf4j;

@Path("/api/v1alpha2")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
@Tag(name = "v1alpha2", description = "v1alpha2 API endpoints (deprecated)")
@PermitAll
@Slf4j
public class SBOMResource extends AbstractApiProvider {

    @Inject
    protected V1Alpha2Mapper mapper;

    @Inject
    FeatureFlags featureFlags;

    @GET
    @Path("/sboms")
    @Operation(summary = "Search SBOMs", description = "List paginated SBOMs using RSQL advanced search.")
    @Parameter(
            name = "query",
            description = "A RSQL query to search the SBOMs",
            examples = {
                    @ExampleObject(name = "Find all SBOMs with provided buildId", value = "buildId=eq=ABCDEFGHIJKLM"),
                    @ExampleObject(
                            name = "Find all SBOMs with provided purl",
                            value = "rootPurl=eq='pkg:maven/com.github.michalszynkiewicz.test/empty@1.0.0.redhat-00270?type=jar'") })
    @Parameter(
            name = "sort",
            description = "Optional RSQL sort",
            examples = { @ExampleObject(name = "Order SBOMs by id in ascending order", value = "id=asc="),
                    @ExampleObject(
                            name = "Order SBOMs by creation time in descending order",
                            value = "creationTime=desc=") })
    @APIResponses({
            @APIResponse(responseCode = "200", description = "List of SBOMs in the system for a specified RSQL query."),
            @APIResponse(
                    responseCode = "400",
                    description = "Failed while parsing the provided RSQL string, please verify the correct syntax.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @APIResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))), })
    public Page<BaseSbomRecord> searchSboms(
            @Valid @BeanParam PaginationParameters paginationParams,
            @QueryParam("query") String rsqlQuery,
            @DefaultValue("creationTime=desc=") @QueryParam("sort") String sort) {

        Page<org.jboss.sbomer.core.dto.BaseSbomRecord> sboms = sbomService.searchSbomRecordsByQueryPaginated(
                paginationParams.getPageIndex(),
                paginationParams.getPageSize(),
                rsqlQuery,
                sort);

        return mapper.toSbomSearchRecordPage(sboms);
    }

    @POST
    @Consumes({ MediaType.APPLICATION_JSON, YAMLMediaTypes.APPLICATION_JACKSON_YAML })
    @Operation(
            summary = "Generate SBOM based on the PNC build",
            description = "SBOM base generation for a particular PNC build Id offloaded to the service.")
    @Parameter(name = "buildId", description = "PNC build identifier", example = "ARYT3LBXDVYAC")
    @Path("/sboms/generate/build/{buildId}")
    @APIResponses({ @APIResponse(
            responseCode = "202",
            description = "Schedules generation of a SBOM for a particular PNC buildId. This is an asynchronous call. It does execute the generation behind the scenes.",
            content = @Content(schema = @Schema(implementation = SbomGenerationRequestRecord.class))),
            @APIResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @APIResponse(
                    responseCode = "400",
                    description = "Could not parse provided arguments",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),

    })
    public Response generate(@PathParam("buildId") String buildId, Config config) throws Exception {
        return Response.accepted(mapper.toSbomRequestRecord(sbomService.generateFromBuild(buildId, config))).build();
    }

    @GET
    @Path("/sboms/{id}")
    @Operation(summary = "Get specific SBOM", description = "Get specific SBOM with the provided ID.")
    @Parameter(name = "id", description = "SBOM identifier", example = "429305915731435500")
    @APIResponses({ //
            @APIResponse(
                    responseCode = "200",
                    description = "The SBOM",
                    content = @Content(schema = @Schema(implementation = SbomRecord.class))),
            @APIResponse(
                    responseCode = "400",
                    description = "Could not parse provided arguments",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @APIResponse(
                    responseCode = "404",
                    description = "Requested SBOM could not be found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @APIResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))), //
    })
    public SbomRecord getSbomById(@PathParam("id") String sbomId) {
        return mapper.toSbomRecord(doGetSbomById(sbomId));
    }

    @GET
    @Path("/sboms/{id}/bom")
    @Operation(
            summary = "Get the BOM content of particular SBOM",
            description = "Get the BOM content of particular SBOM")
    @Parameter(name = "id", description = "SBOM identifier", example = "429305915731435500")
    @APIResponses({ //
            @APIResponse(
                    responseCode = "200",
                    description = "The BOM in CycloneDX format",
                    content = @Content(schema = @Schema(implementation = Map.class))),
            @APIResponse(
                    responseCode = "400",
                    description = "Could not parse provided arguments",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @APIResponse(
                    responseCode = "404",
                    description = "Requested SBOM could not be found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @APIResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))), })
    public JsonNode getBomById(@PathParam("id") String sbomId) {
        return doGetBomById(sbomId);
    }

    @GET
    @Path("/sboms/purl/{purl}")
    @Operation(summary = "Get specific SBOM", description = "Find latest generated SBOM for a given purl.")
    @Parameter(
            name = "purl",
            description = "Package URL identifier",
            example = "scheme:type/namespace/name@version?qualifiers#subpath")
    @APIResponses({ @APIResponse(responseCode = "200", description = "The SBOM"),
            @APIResponse(responseCode = "400", description = "Could not parse provided arguments"),
            @APIResponse(responseCode = "404", description = "Requested SBOM could not be found"),
            @APIResponse(responseCode = "500", description = "Internal server error"), })
    public SbomRecord getSbomByPurl(@PathParam("purl") String purl) {
        return mapper.toSbomRecord(doGetSbomByPurl(purl));
    }

    @GET
    @Path("/sboms/purl/{purl}/bom")
    @Operation(
            summary = "Get the BOM content of particular SBOM identified by provided purl",
            description = "Returns the CycloneDX BOM content of particular SBOM identified by provided purl")
    @Parameter(
            name = "purl",
            description = "Package URL identifier",
            example = "scheme:type/namespace/name@version?qualifiers#subpath")
    @APIResponses({
            @APIResponse(
                    responseCode = "200",
                    description = "The BOM in CycloneDX format",
                    content = @Content(schema = @Schema(implementation = Map.class))),
            @APIResponse(responseCode = "400", description = "Could not parse provided arguments"),
            @APIResponse(responseCode = "404", description = "Requested SBOM could not be found"),
            @APIResponse(responseCode = "500", description = "Internal server error"), })
    public JsonNode getBomByPurl(@PathParam("purl") String purl) {
        return doGetBomByPurl(purl);
    }

    @GET
    @Path("/sboms/requests")
    @Operation(
            summary = "List SBOM generation requests",
            description = "Paginated list of SBOM generation requests using RSQL advanced search.")
    @Parameter(
            name = "query",
            description = "A RSQL query to search the generation requests",
            examples = { @ExampleObject(
                    name = "Find all SBOM generation requests with provided buildId",
                    value = "buildId=eq=ABCDEFGHIJKLM") })
    @Parameter(
            name = "sort",
            description = "Optional RSQL sort",
            examples = { @ExampleObject(name = "Order generation requests by id in ascending order", value = "id=asc="),
                    @ExampleObject(
                            name = "Order generation requests by creation time in descending order",
                            value = "creationTime=desc=") })
    @APIResponses({
            @APIResponse(
                    responseCode = "200",
                    description = "List of SBOM generation requests in the system for a specified RSQL query."),
            @APIResponse(
                    responseCode = "400",
                    description = "Failed while parsing the provided RSQL string, please verify the correct syntax.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @APIResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))), })
    public Page<SbomGenerationRequestRecord> searchGenerationRequests(
            @Valid @BeanParam PaginationParameters paginationParams,
            @QueryParam("query") String rsqlQuery,
            @DefaultValue("creationTime=desc=") @QueryParam("sort") String sort) {

        Page<SbomGenerationRequest> requests = sbomService.searchSbomRequestsByQueryPaginated(
                paginationParams.getPageIndex(),
                paginationParams.getPageSize(),
                rsqlQuery,
                sort);

        return mapper.toSbomRequestRecordPage(requests);
    }

    @GET
    @Path("/sboms/requests/{id}")
    @Operation(
            summary = "Get specific SBOM generation request",
            description = "Get specific SBOM generation request with the provided ID.")
    @Parameter(name = "id", description = "SBOM generation request identifier", example = "88CA2291D4014C6")
    @APIResponses({ @APIResponse(responseCode = "200", description = "The generation request"),
            @APIResponse(responseCode = "400", description = "Could not parse provided arguments"),
            @APIResponse(
                    responseCode = "404",
                    description = "Requested generation request could not be found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @APIResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))), })
    public SbomGenerationRequestRecord getGenerationRequestById(@PathParam("id") String generationRequestId) {
        return mapper.toSbomRequestRecord(doGetSbomGenerationRequestById(generationRequestId));
    }

    @POST
    @Operation(
            summary = "Resend UMB notification message for a completed SBOM",
            description = "Force the resending of the UMB notification message for an already generated SBOM.")
    @Parameter(name = "id", description = "SBOM identifier", example = "429305915731435500")
    @Path("/sboms/{id}/notify")
    @APIResponses({ @APIResponse(responseCode = "200"),
            @APIResponse(
                    responseCode = "404",
                    description = "Requested SBOM could not be found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON)),
            @APIResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON)), })
    public Response notify(@PathParam("id") String sbomId) throws Exception {
        if (featureFlags.isDryRun()) {
            log.warn("Skipping notification for SBOM '{}' because of SBOMer running in dry-run mode", sbomId);
            return Response.status(Status.METHOD_NOT_ALLOWED).build();
        }

        Sbom sbom = doGetSbomById(sbomId);
        sbomService.notifyCompleted(sbom);
        return Response.ok().build();
    }

}
