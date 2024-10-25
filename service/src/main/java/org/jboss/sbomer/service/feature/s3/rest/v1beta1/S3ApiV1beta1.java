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
package org.jboss.sbomer.service.feature.s3.rest.v1beta1;

import java.util.List;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.sbomer.core.errors.ErrorResponse;
import org.jboss.sbomer.core.errors.ServiceUnavailableException;
import org.jboss.sbomer.service.feature.FeatureFlags;
import org.jboss.sbomer.service.feature.s3.S3StorageHandler;

import com.fasterxml.jackson.jakarta.rs.yaml.YAMLMediaTypes;

import jakarta.annotation.security.PermitAll;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;

@Path("/api/v1beta1")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
@PermitAll
@Slf4j
@Tag(name = "v1beta1")
public class S3ApiV1beta1 {
    @Inject
    FeatureFlags featureFlags;

    @Inject
    S3StorageHandler s3StorageHandler;

    @GET
    @Consumes({ MediaType.APPLICATION_JSON, YAMLMediaTypes.APPLICATION_JACKSON_YAML })
    @Operation(summary = "List all log file paths for a given GenerationRequest", description = "")
    @Path("/requests/{id}/logs")
    @APIResponse(
            responseCode = "200",
            description = "List of paths to log files available for a given GenerationRequest")

    @APIResponse(
            responseCode = "404",
            description = "Given GenerationRequest could not be found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @APIResponse(
            responseCode = "500",
            description = "Internal server error",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @APIResponse(
            responseCode = "503",
            description = "Content cannot be returned at this time",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public Response getLog(@PathParam("id") String generationRequestId) throws Exception {
        if (!featureFlags.s3Storage()) {
            throw new ServiceUnavailableException("S3 feature is disabled currently, try again later");
        }

        log.info("Fetching list of paths to log files for GenerationRequest '{}'", generationRequestId);

        List<String> paths = s3StorageHandler.listLogFilesInBucket(generationRequestId);

        return Response.ok(paths).build();
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Consumes({ MediaType.APPLICATION_JSON, YAMLMediaTypes.APPLICATION_JACKSON_YAML })
    @Operation(summary = "Fetch generation log on a specified path", description = "")
    @Path("/requests/{id}/logs/{path}")
    @APIResponse(
            responseCode = "200",
            description = "Requests manifest generation for a given container image.",
            content = @Content(mediaType = MediaType.TEXT_PLAIN))
    @APIResponse(
            responseCode = "500",
            description = "Internal server error",
            content = @Content(mediaType = MediaType.APPLICATION_JSON))
    public Response getLog(@PathParam("id") String generationRequestId, @PathParam("path") String path)
            throws Exception {
        if (!featureFlags.s3Storage()) {
            throw new ServiceUnavailableException("S3 feature is disabled currently, try again later");
        }

        log.info("Fetching log for GenerationRequest '{}' on path '{}'", generationRequestId, path);

        String log = s3StorageHandler.getLog(generationRequestId, path);

        return Response.ok(log).build();
    }
}
