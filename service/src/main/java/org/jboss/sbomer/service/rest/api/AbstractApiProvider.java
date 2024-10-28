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
package org.jboss.sbomer.service.rest.api;

import static org.jboss.sbomer.service.feature.sbom.UserRoles.USER_DELETE_ROLE;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.jboss.sbomer.core.errors.NotFoundException;
import org.jboss.sbomer.core.features.sbom.utils.MDCUtils;
import org.jboss.sbomer.service.feature.FeatureFlags;
import org.jboss.sbomer.service.feature.sbom.model.Sbom;
import org.jboss.sbomer.service.feature.sbom.model.SbomGenerationRequest;
import org.jboss.sbomer.service.feature.sbom.service.SbomService;

import com.fasterxml.jackson.databind.JsonNode;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractApiProvider {

    @Inject
    protected SbomService sbomService;

    @Inject
    FeatureFlags featureFlags;

    protected Sbom doGetSbomByPurl(String purl) {
        Sbom sbom = sbomService.findByPurl(purl);

        if (sbom == null) {
            throw new NotFoundException("SBOM with purl = '" + purl + "' couldn't be found");
        }

        return sbom;
    }

    protected Sbom doGetSbomById(String sbomId) {
        Sbom sbom = sbomService.get(sbomId);

        if (sbom == null) {
            throw new NotFoundException("SBOM with id '{}' not found", sbomId);
        }

        return sbom;
    }

    protected JsonNode doGetBomById(String sbomId) {
        Sbom sbom = doGetSbomById(sbomId);
        return sbom.getSbom();
    }

    protected JsonNode doGetBomByPurl(String purl) {
        Sbom sbom = doGetSbomByPurl(purl);
        return sbom.getSbom();
    }

    protected SbomGenerationRequest doGetSbomGenerationRequestById(String generationRequestId) {
        SbomGenerationRequest generationRequest = SbomGenerationRequest.findById(generationRequestId); // NOSONAR

        if (generationRequest == null) {
            throw new NotFoundException("Generation request with id '{}' could not be found", generationRequestId);
        }

        return generationRequest;
    }

    @DELETE
    @Path("/sboms/requests/{id}")
    @RolesAllowed(USER_DELETE_ROLE)
    @Operation(
            summary = "Delete SBOM generation request specified by id",
            description = "Delete the specified SBOM generation request from the database")
    @Parameter(name = "id", description = "The SBOM request identifier")
    @APIResponse(responseCode = "200", description = "SBOM generation request was successfully deleted")
    @APIResponse(responseCode = "404", description = "Specified SBOM generation request could not be found")
    @APIResponse(responseCode = "500", description = "Internal server error")
    public Response deleteGenerationRequest(@PathParam("id") final String id) {

        try {
            MDCUtils.addProcessContext(id);
            sbomService.deleteSbomRequest(id);

            return Response.ok().build();
        } finally {
            MDCUtils.removeProcessContext();
        }
    }

    @POST
    @Operation(
            summary = "Resend UMB notification message for a completed SBOM",
            description = "Force the resending of the UMB notification message for an already generated SBOM.")
    @Parameter(name = "id", description = "SBOM identifier", example = "429305915731435500")
    @Path("/sboms/{id}/notify")
    @APIResponse(responseCode = "200")
    @APIResponse(
            responseCode = "404",
            description = "Requested SBOM could not be found",
            content = @Content(mediaType = MediaType.APPLICATION_JSON))
    @APIResponse(
            responseCode = "500",
            description = "Internal server error",
            content = @Content(mediaType = MediaType.APPLICATION_JSON))
    public Response notify(@PathParam("id") String sbomId) throws Exception {
        if (featureFlags.isDryRun()) {
            log.warn("Skipping notification for SBOM '{}' because of SBOMer running in dry-run mode", sbomId);
            return Response.status(Status.SERVICE_UNAVAILABLE).build();
        }

        Sbom sbom = doGetSbomById(sbomId);
        sbomService.notifyCompleted(sbom);
        return Response.ok().build();
    }
}
