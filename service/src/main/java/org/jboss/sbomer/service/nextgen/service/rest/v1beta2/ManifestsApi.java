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
package org.jboss.sbomer.service.nextgen.service.rest.v1beta2;

import java.util.List;

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
import org.jboss.sbomer.service.nextgen.core.dto.model.ManifestRecord;
import org.jboss.sbomer.service.nextgen.service.EntityMapper;
import org.jboss.sbomer.service.nextgen.service.model.Manifest;

import io.vertx.core.eventbus.EventBus;
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
import lombok.extern.slf4j.Slf4j;

@Path("/api/v1beta2/manifests")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
@PermitAll
@Tag(name = "v1beta2")
@Slf4j
public class ManifestsApi {
    @Inject
    EntityMapper mapper;

    @Inject
    EventBus eventBus;

    @GET
    @Operation(
            summary = "Search manifests",
            description = "Performs a query according to the search criteria and returns paginated list of manifests")
    @APIResponse(
            responseCode = "200",
            description = "Paginated list of manifests",
            content = @Content(mediaType = MediaType.APPLICATION_JSON))
    @APIResponse(
            responseCode = "500",
            description = "Internal server error",
            content = @Content(mediaType = MediaType.APPLICATION_JSON))
    public List<ManifestRecord> search( // TODO: USE pagination
            @Valid @BeanParam PaginationParameters paginationParams,
            @QueryParam("query") String query,
            @DefaultValue("creationTime=desc=") @QueryParam("sort") String sort) {

        List<Manifest> manifests = Manifest.listAll();

        return mapper.toManifestRecords(manifests);
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Get specific manifest", description = "Get manifest by the identifier")
    @Parameter(
            name = "id",
            description = "Manifest identifier",
            examples = { @ExampleObject(value = "88CA2291D4014C6", name = "Manifest identifier") })
    @APIResponse(
            responseCode = "200",
            description = "Event content",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = ManifestRecord.class)))
    @APIResponse(
            responseCode = "400",
            description = "Malformed request",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = ErrorResponse.class)))
    @APIResponse(
            responseCode = "404",
            description = "Manifest could not be found",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = ErrorResponse.class)))
    @APIResponse(
            responseCode = "500",
            description = "Internal server error",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = ErrorResponse.class)))
    public ManifestRecord getById(@PathParam("id") String manifestId) {
        Manifest manifest = Manifest.findById(manifestId); // NOSONAR

        if (manifest == null) {
            throw new NotFoundException("Manifest with id '{}' could not be found", manifestId);
        }

        return mapper.toRecord(manifest);
    }
}
