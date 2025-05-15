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
package org.jboss.sbomer.service.rest.api.v1beta2;

import java.util.List;
import java.util.Map;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.ExampleObject;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.sbomer.core.errors.ErrorResponse;
import org.jboss.sbomer.core.errors.NotFoundException;
import org.jboss.sbomer.core.utils.PaginationParameters;
import org.jboss.sbomer.service.feature.FeatureFlags;
import org.jboss.sbomer.service.feature.sbom.model.v1beta2.Generation;
import org.jboss.sbomer.service.feature.sbom.service.SbomService;

import jakarta.annotation.security.PermitAll;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;

@Path("/api/v1beta2/generations")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
@PermitAll
@Tag(name = "v1beta2")
@Slf4j
public class GenerationsV1Beta2 {
    @Inject
    SbomService sbomService;

    @Inject
    FeatureFlags featureFlags;

    @GET
    @Path("/{id}")
    @Operation(
            summary = "Get specific manifest generation",
            description = "Get generation for the provided identifier or purl")
    @Parameter(
            name = "id",
            description = "Manifest generation identifier",
            examples = { @ExampleObject(value = "88CA2291D4014C6", name = "Generation identifier") })
    @APIResponse(
            responseCode = "200",
            description = "The generation",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = Map.class))) // TODO:
                                                                                                                      // populate
                                                                                                                      // it
    @APIResponse(
            responseCode = "400",
            description = "Malformed request",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = ErrorResponse.class)))
    @APIResponse(
            responseCode = "404",
            description = "Generation could not be found",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = ErrorResponse.class)))
    @APIResponse(
            responseCode = "500",
            description = "Internal server error",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = ErrorResponse.class)))
    public Response getGenerationRequestById(@PathParam("id") String generationId) {
        Generation generation = Generation.findById(generationId); // NOSONAR

        if (generation == null) {
            throw new NotFoundException("Generation request with id '{}' could not be found", generationId);
        }

        return Response.ok(generation).build(); // TODO: USE DTO!
    }

    @GET
    @Operation(summary = "Search manifest generation", description = "Paginated list of generations")
    @APIResponse(
            responseCode = "200",
            description = "Paginated list of generations found",
            content = @Content(mediaType = MediaType.APPLICATION_JSON))
    @APIResponse(
            responseCode = "500",
            description = "Internal server error",
            content = @Content(mediaType = MediaType.APPLICATION_JSON))
    public Response search( // TODO: USE paging
            @Valid @BeanParam PaginationParameters paginationParams,
            @QueryParam("query") String rsqlQuery,
            @DefaultValue("creationTime=desc=") @QueryParam("sort") String sort) {

        List<Generation> generations = Generation.findAll().list();

        generations.forEach(generation -> {
            log.info("Tasks: {}", generation.getTasks().stream().map(t -> t.getId()).toList());
        });

        return Response.ok(generations).build();

    }

}
