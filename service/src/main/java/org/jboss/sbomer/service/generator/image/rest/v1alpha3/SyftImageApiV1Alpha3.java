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
package org.jboss.sbomer.service.generator.image.rest.v1alpha3;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.sbomer.core.config.request.ImageRequestConfig;
import org.jboss.sbomer.core.dto.v1alpha3.SbomGenerationRequestRecord;
import org.jboss.sbomer.core.features.sbom.config.SyftImageConfig;
import org.jboss.sbomer.service.feature.FeatureFlags;
import org.jboss.sbomer.service.feature.sbom.model.RequestEvent;
import org.jboss.sbomer.service.feature.sbom.service.SbomService;
import org.jboss.sbomer.service.rest.RestUtils;
import org.jboss.sbomer.service.rest.mapper.V1Alpha3Mapper;

import com.fasterxml.jackson.jakarta.rs.yaml.YAMLMediaTypes;

import io.opentelemetry.api.trace.Span;
import jakarta.annotation.security.PermitAll;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/api/v1alpha3/generator/syft")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
@PermitAll
@Tag(name = "v1alpha3")
@Deprecated
public class SyftImageApiV1Alpha3 {
    @Inject
    V1Alpha3Mapper mapper;

    @Inject
    SbomService sbomService;

    @Inject
    FeatureFlags featureFlags;

    @POST
    @Consumes({ MediaType.APPLICATION_JSON, YAMLMediaTypes.APPLICATION_JACKSON_YAML })
    @Operation(summary = "", description = "")
    @Path("/image/{name}")
    @APIResponse(
            responseCode = "202",
            description = "Requests manifest generation for a given container image.",
            content = @Content(schema = @Schema(implementation = SbomGenerationRequestRecord.class)))
    @APIResponse(
            responseCode = "500",
            description = "Internal server error",
            content = @Content(mediaType = MediaType.APPLICATION_JSON))
    public Response generateFromContainerImage(
            @PathParam("name") String imageName,
            SyftImageConfig config,
            @Context ContainerRequestContext requestContext) throws Exception {

        if (config == null) {
            config = new SyftImageConfig();
        }
        config.setImage(imageName);

        // Create the Request to be associated with this REST API call event
        RequestEvent request = RestUtils.createRequestFromRestEvent(
                ImageRequestConfig.builder().withImage(imageName).build(),
                requestContext,
                Span.current());

        return Response.accepted(mapper.toRecord(sbomService.generateSyftImage(request, config))).build();
    }
}
