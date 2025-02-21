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
package org.jboss.sbomer.service.rest.api.v1beta1;

import java.util.Map;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.ExampleObject;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.sbomer.core.dto.BaseSbomRecord;
import org.jboss.sbomer.core.dto.v1beta1.V1Beta1BaseManifestRecord;
import org.jboss.sbomer.core.dto.v1beta1.V1Beta1ManifestRecord;
import org.jboss.sbomer.core.errors.ErrorResponse;
import org.jboss.sbomer.core.errors.NotFoundException;
import org.jboss.sbomer.core.features.sbom.rest.Page;
import org.jboss.sbomer.core.utils.PaginationParameters;
import org.jboss.sbomer.service.feature.FeatureFlags;
import org.jboss.sbomer.service.feature.sbom.model.Sbom;
import org.jboss.sbomer.service.feature.sbom.service.SbomService;
import org.jboss.sbomer.service.rest.mapper.V1Beta1Mapper;

import com.fasterxml.jackson.databind.JsonNode;

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

@Path("/api/v1beta1/manifests")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
@PermitAll
@Slf4j
@Tag(name = "v1beta1")
public class ManifestsV1Beta1 {
    @Inject
    V1Beta1Mapper mapper;

    @Inject
    SbomService sbomService;

    @Inject
    FeatureFlags featureFlags;

    @GET
    @Operation(summary = "Search manifests", description = "List paginated manifests using RSQL advanced search.")
    @Parameter(
            name = "query",
            description = "A RSQL query to search manifests",
            examples = { @ExampleObject(
                    name = "Find all manifests with provided purl",
                    value = "rootPurl=eq='pkg:maven/com.github.michalszynkiewicz.test/empty@1.0.0.redhat-00270?type=jar'") })
    @Parameter(
            name = "sort",
            description = "Optional RSQL sort",
            examples = { @ExampleObject(name = "Order manifests by id in ascending order", value = "id=asc="),
                    @ExampleObject(
                            name = "Order manifests by creation time in descending order",
                            value = "creationTime=desc=") })
    @APIResponse(
            responseCode = "200",
            description = "Paginated list of manifests in the system for a specified RSQL query.")
    @APIResponse(
            responseCode = "400",
            description = "Failed while parsing the provided RSQL string, please verify the correct syntax.",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @APIResponse(
            responseCode = "500",
            description = "Internal server error",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public Page<V1Beta1BaseManifestRecord> searchSboms(
            @Valid @BeanParam PaginationParameters paginationParams,
            @QueryParam("query") String rsqlQuery,
            @DefaultValue("creationTime=desc=") @QueryParam("sort") String sort) {

        Page<BaseSbomRecord> sboms = sbomService.searchSbomRecordsByQueryPaginated(
                paginationParams.getPageIndex(),
                paginationParams.getPageSize(),
                rsqlQuery,
                sort);

        return mapper.toRecord(sboms);
    }

    @GET
    @Path("/{id}")
    @Operation(
            summary = "Get specific manifest",
            description = "Get specific manifest for the provided identifier. Both; manifest identifier and purl's are supported.")
    @Parameter(
            name = "id",
            description = "Manifest generation request identifier or purl",
            examples = { @ExampleObject(value = "88CA2291D4014C6", name = "Generation request identifier"),
                    @ExampleObject(
                            value = "pkg:maven/com.github.michalszynkiewicz.test/empty@1.0.0.redhat-00270?type=jar",
                            name = "Package URL") })
    @APIResponse(
            responseCode = "200",
            description = "The manifest",
            content = @Content(schema = @Schema(implementation = V1Beta1ManifestRecord.class)))
    @APIResponse(
            responseCode = "400",
            description = "Could not parse provided arguments",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @APIResponse(
            responseCode = "404",
            description = "Requested SBOM could not be found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @APIResponse(
            responseCode = "500",
            description = "Internal server error",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public V1Beta1ManifestRecord getSbomById(@PathParam("id") String identifier) {
        Sbom sbom = sbomService.get(identifier);

        if (sbom == null) {
            sbom = sbomService.findByPurl(identifier);
        }

        if (sbom == null) {
            throw new NotFoundException("Manifest with provided identifier: '" + identifier + "' couldn't be found");
        }

        return mapper.toRecord(sbom);
    }

    @GET
    @Path("/{id}/bom")
    @Operation(
            summary = "Get the BOM content in CycloneDX format of particular manifest",
            description = "Get the BOM content of particular manifest")
    @Parameter(
            name = "id",
            description = "Manifest generation request identifier or purl",
            examples = { @ExampleObject(value = "88CA2291D4014C6", name = "Manifest identifier"),
                    @ExampleObject(
                            value = "pkg:maven/com.github.michalszynkiewicz.test/empty@1.0.0.redhat-00270?type=jar",
                            name = "Package URL") })
    @APIResponse(
            responseCode = "200",
            description = "The BOM in CycloneDX format",
            content = @Content(schema = @Schema(implementation = Map.class)))
    @APIResponse(
            responseCode = "400",
            description = "Could not parse provided arguments",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @APIResponse(
            responseCode = "404",
            description = "Requested manifest could not be found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @APIResponse(
            responseCode = "500",
            description = "Internal server error",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public JsonNode getBomById(@PathParam("id") String identifier) {
        Sbom sbom = sbomService.get(identifier);

        if (sbom == null) {
            sbom = sbomService.findByPurl(identifier);
        }

        if (sbom == null) {
            throw new NotFoundException(
                    "Manifest with could not be found for provided identifier: '" + identifier + "'");
        }

        // TODO: We probably should ensure proper formatting (ordering of keys)
        return sbom.getSbom();
    }

    @POST
    @Consumes(MediaType.WILDCARD)
    @Operation(
            summary = "Resend UMB notification message for a generated manifest",
            description = "Force the resending of the UMB notification message for an already generated SBOM.")
    @Parameter(name = "id", description = "Manifest identifier", example = "429305915731435500")
    @Path("/{id}/notify")
    @APIResponse(responseCode = "200", content = @Content(mediaType = MediaType.APPLICATION_JSON))
    @APIResponse(
            responseCode = "404",
            description = "Requested manifest could not be found",
            content = @Content(mediaType = MediaType.APPLICATION_JSON))
    @APIResponse(
            responseCode = "500",
            description = "Internal server error",
            content = @Content(mediaType = MediaType.APPLICATION_JSON))
    public Response notify(@PathParam("id") String sbomId) {
        if (featureFlags.isDryRun()) {
            log.warn("Skipping notification for manifest '{}' because of SBOMer running in dry-run mode", sbomId);
            return Response.status(Status.SERVICE_UNAVAILABLE).build();
        }

        Sbom sbom = sbomService.get(sbomId);

        if (sbom == null) {
            throw new NotFoundException("Manifest with provided identifier: '{}' couldn't be found", sbomId);
        }

        sbomService.notifyCompleted(sbom);
        return Response.ok().build();
    }
}
