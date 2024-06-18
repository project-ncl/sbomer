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
package org.jboss.sbomer.service.feature.sbom.features.generator.image.syft.rest;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.jboss.sbomer.core.dto.v1alpha3.SbomGenerationRequestRecord;
import org.jboss.sbomer.core.features.sbom.config.runtime.Config;
import org.jboss.sbomer.core.features.sbom.enums.GenerationRequestType;
import org.jboss.sbomer.service.feature.sbom.features.FeatureFlags;
import org.jboss.sbomer.service.feature.sbom.k8s.model.GenerationRequest;
import org.jboss.sbomer.service.feature.sbom.k8s.model.GenerationRequestBuilder;
import org.jboss.sbomer.service.feature.sbom.k8s.model.SbomGenerationStatus;
import org.jboss.sbomer.service.feature.sbom.mapper.V1Alpha3Mapper;
import org.jboss.sbomer.service.feature.sbom.model.SbomGenerationRequest;

import com.fasterxml.jackson.jakarta.rs.yaml.YAMLMediaTypes;

import io.fabric8.kubernetes.client.KubernetesClient;
import jakarta.annotation.security.PermitAll;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;

@Path("/api/v1alpha3/generator/syft")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
@PermitAll
@Slf4j
public class SBOMResource {
    @Inject
    V1Alpha3Mapper mapper;

    @Inject
    protected KubernetesClient kubernetesClient;

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
    public Response generateFromContainerImage(@PathParam("name") String imageName, Config config) throws Exception {
        log.info("Requesting new manifest generation for container image: '{}'", imageName);

        GenerationRequest req = new GenerationRequestBuilder(GenerationRequestType.CONTAINERIMAGE)
                .withIdentifier(imageName)
                .withStatus(SbomGenerationStatus.NEW)
                .build();

        SbomGenerationRequest sbomGenerationRequest = SbomGenerationRequest.sync(req);

        kubernetesClient.configMaps().resource(req).create();

        return Response.accepted(mapper.toSbomRequestRecord(sbomGenerationRequest)).build();
    }
}
