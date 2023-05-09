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
package org.jboss.sbomer.cli.client;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.jboss.sbomer.cli.model.Sbom;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Client used to interact with the SBOMer REST API.
 */
@ApplicationScoped
@RegisterRestClient(configKey = "sbomer")
@RegisterProvider(ClientExceptionMapper.class)
@Path("/api/v1alpha1/sboms")
@Produces("application/json")
@Consumes("application/json")
public interface SBOMerClient {
    /**
     * Saves the SBOM in the service.
     */
    @POST
    @Path("/{id}/bom")
    String updateSbom(@PathParam("id") String sbomId, JsonNode bom);

    /**
     * Retrieves SBOM based on the ID.
     *
     * @param id
     * @return {@link Sbom}
     */
    @GET
    @Path("/{id}")
    Sbom getById(@PathParam("id") String id);

    /**
     * Retrieves the base SBOM based on the build ID.
     *
     * @param buildId
     * @return {@link Sbom}
     */
    @GET
    @Path("/build/{buildId}")
    Sbom getBaseSbomWithPncBuildId(@PathParam("buildId") String buildId);
}
