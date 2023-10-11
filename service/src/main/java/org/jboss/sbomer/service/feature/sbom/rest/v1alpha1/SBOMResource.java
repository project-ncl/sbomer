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
package org.jboss.sbomer.service.feature.sbom.rest.v1alpha1;

import static org.jboss.sbomer.service.feature.sbom.UserRoles.SYSTEM_USER;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.ExampleObject;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.sbomer.core.SchemaValidator.ValidationResult;
import org.jboss.sbomer.core.config.ConfigSchemaValidator;
import org.jboss.sbomer.core.config.SbomerConfigProvider;
import org.jboss.sbomer.core.errors.NotFoundException;
import org.jboss.sbomer.core.errors.ValidationException;
import org.jboss.sbomer.core.features.sbom.config.runtime.Config;
import org.jboss.sbomer.core.features.sbom.rest.Page;
import org.jboss.sbomer.core.features.sbom.utils.MDCUtils;
import org.jboss.sbomer.core.features.sbom.utils.ObjectMapperProvider;
import org.jboss.sbomer.core.features.sbom.utils.SbomUtils;
import org.jboss.sbomer.core.utils.PaginationParameters;
import org.jboss.sbomer.service.feature.sbom.k8s.model.GenerationRequest;
import org.jboss.sbomer.service.feature.sbom.k8s.model.GenerationRequestBuilder;
import org.jboss.sbomer.service.feature.sbom.k8s.model.SbomGenerationStatus;
import org.jboss.sbomer.service.feature.sbom.model.Sbom;
import org.jboss.sbomer.service.feature.sbom.model.SbomGenerationRequest;
import org.jboss.sbomer.service.feature.sbom.service.SbomService;

import com.fasterxml.jackson.jakarta.rs.yaml.YAMLMediaTypes;

import cz.jirutka.rsql.parser.RSQLParserException;
import io.fabric8.kubernetes.client.KubernetesClient;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
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

@Path("/api/v1alpha1/sboms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
@Tag(name = "v1alpha1", description = "v1alpha1 API endpoints")
@PermitAll
@Slf4j
public class SBOMResource {

    @Inject
    protected SbomService sbomService;

    @Inject
    KubernetesClient kubernetesClient;

    @Inject
    ConfigSchemaValidator configSchemaValidator;

    // RSQL Examples:
    // -------------------------------------------------------------------------------
    // ==> "/api/v1alpha1/sboms?query=buildId==eq={buildId}&sort=id=asc="
    // ==> "/api/v1alpha1/sboms?query=rootPurl=eq='{rootPurl}'"
    // -------------------------------------------------------------------------------

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
    @Parameter(
            name = "sort",
            description = "Optional RSQL sort",
            examples = { @ExampleObject(name = "Order SBOMs by id in ascending order", value = "id=asc="),
                    @ExampleObject(
                            name = "Order SBOMs by creation time in descending order",
                            value = "creationTime=desc=") })
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
            @QueryParam("query") String rsqlQuery,
            @DefaultValue("creationTime=desc=") @QueryParam("sort") String sort) {

        try {
            Page<Sbom> sboms = sbomService.searchSbomsByQueryPaginated(
                    paginationParams.getPageIndex(),
                    paginationParams.getPageSize(),
                    rsqlQuery,
                    sort);
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

    private Sbom doGetBomById(String sbomId) {
        Sbom sbom = sbomService.get(sbomId);

        if (sbom == null) {
            throw new NotFoundException("SBOM with id '" + sbomId + "' not found");
        }

        return sbom;
    }

    @POST
    @Consumes({ MediaType.APPLICATION_JSON, YAMLMediaTypes.APPLICATION_JACKSON_YAML })
    @Operation(
            summary = "Generate SBOM based on the PNC build",
            description = "SBOM base generation for a particular PNC build Id offloaded to the service.")
    @Parameter(name = "buildId", description = "PNC build identifier", example = "ARYT3LBXDVYAC")
    @Path("/generate/build/{buildId}")
    @APIResponses({ @APIResponse(
            responseCode = "202",
            description = "Schedules generation of a SBOM for a particular PNC buildId. This is an asynchronous call. It does execute the generation behind the scenes.",
            content = @Content(mediaType = MediaType.APPLICATION_JSON)),
            @APIResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON)) })

    public Response generate(@PathParam("buildId") String buildId, Config config) throws Exception {

        try {
            MDCUtils.addBuildContext(buildId);

            log.info("New generation request for build id '{}'", buildId);
            log.debug("Creating GenerationRequest Kubernetes resource...");

            GenerationRequest req = new GenerationRequestBuilder().withNewDefaultMetadata(buildId)
                    .endMetadata()
                    .withBuildId(buildId)
                    .withStatus(SbomGenerationStatus.NEW)
                    .build();

            if (config != null) {
                log.debug("Received product configuration...");

                SbomerConfigProvider sbomerConfigProvider = SbomerConfigProvider.getInstance();
                sbomerConfigProvider.adjust(config);
                config.setBuildId(buildId);

                ValidationResult validationResult = configSchemaValidator.validate(config);

                if (!validationResult.isValid()) {
                    throw new ValidationException("Provided config is not valid", validationResult.getErrors());
                }

                // Because the config is valid, use it and set the status to initialized
                req.setStatus(SbomGenerationStatus.INITIALIZED);
                req.setConfig(ObjectMapperProvider.json().writeValueAsString(config));
            }

            SbomGenerationRequest sbomGenerationRequest = SbomGenerationRequest.sync(req);

            kubernetesClient.configMaps().resource(req).create();

            log.debug("GenerationRequest Kubernetes resource '{}' created for build '{}'", req.getId(), buildId);

            return Response.status(Status.ACCEPTED).entity(sbomGenerationRequest).build();
        } finally {
            MDCUtils.removeBuildContext();
        }
    }

    @GET
    @Path("/requests")
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
                    description = "List of SBOM generation requests in the system for a specified RSQL query.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON)),
            @APIResponse(
                    responseCode = "400",
                    description = "Failed while parsing the provided RSQL string, please verify the correct syntax.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON)),
            @APIResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON)) })
    public Response searchGenerationRequests(
            @Valid @BeanParam PaginationParameters paginationParams,
            @QueryParam("query") String rsqlQuery,
            @DefaultValue("creationTime=desc=") @QueryParam("sort") String sort) {

        try {

            Page<SbomGenerationRequest> requests = sbomService.searchSbomRequestsByQueryPaginated(
                    paginationParams.getPageIndex(),
                    paginationParams.getPageSize(),
                    rsqlQuery,
                    sort);
            return Response.status(Status.OK).entity(requests).build();
        } catch (IllegalArgumentException iae) {
            return Response.status(Status.BAD_REQUEST).entity(iae.getMessage()).build();
        } catch (RSQLParserException rsqlExc) {
            return Response.status(Status.BAD_REQUEST)
                    .entity("Failed while parsing the provided RSQL string, please verify the correct syntax")
                    .build();
        }
    }

    @GET
    @Path("/requests/{id}")
    @Operation(
            summary = "Get specific SBOM generation request",
            description = "Get specific SBOM generation request with the provided ID.")
    @Parameter(name = "id", description = "SBOM generation request identifier", example = "88CA2291D4014C6")
    @APIResponses({
            @APIResponse(
                    responseCode = "200",
                    description = "The generation request",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON)),
            @APIResponse(
                    responseCode = "400",
                    description = "Could not parse provided arguments",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON)),
            @APIResponse(
                    responseCode = "404",
                    description = "Requested generation request could not be found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON)),
            @APIResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON)), })
    public Response getGenerationRequestById(@PathParam("id") String id) {
        SbomGenerationRequest sbomGenerationRequest = SbomGenerationRequest.findById(id);

        if (sbomGenerationRequest == null) {
            return Response.status(Status.NOT_FOUND).build();
        }

        return Response.status(Status.OK).entity(sbomGenerationRequest).build();
    }

    @DELETE
    @Path("/requests/{id}")
    @RolesAllowed(SYSTEM_USER)
    @Operation(
            summary = "Delete SBOM generation request specified by id",
            description = "Delete the specified SBOM generation request from the database")
    @Parameter(name = "id", description = "The SBOM request identifier")
    @APIResponses({
            @APIResponse(
                    responseCode = "404",
                    description = "Specified SBOM generation request could not be found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON)),
            @APIResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON)) })
    public Response deleteGenerationRequest(@PathParam("id") final String id) {

        try {
            MDCUtils.addProcessContext(id);
            sbomService.deleteSbomRequest(id);

            return Response.ok().build();
        } finally {
            MDCUtils.removeProcessContext();
        }
    }

}