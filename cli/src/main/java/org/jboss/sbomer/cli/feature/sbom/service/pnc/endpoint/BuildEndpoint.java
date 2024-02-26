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
package org.jboss.sbomer.cli.feature.sbom.service.pnc.endpoint;

import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.pnc.dto.Artifact;
import org.jboss.pnc.dto.Build;
import org.jboss.pnc.dto.response.Page;
import org.jboss.sbomer.cli.feature.sbom.service.pnc.PageParameters;

import jakarta.validation.Valid;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Tag(name = "Builds")
@Path("/builds")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
// @Client
public interface BuildEndpoint {
    static final String B_ID = "ID of the build";

    @GET
    @Path("/{id}")
    Build getSpecific(@Parameter(description = B_ID) @PathParam("id") String id);

    @GET
    @Path("/{id}/artifacts/dependencies")
    // @TimedMetric
    Page<Artifact> getDependencyArtifacts(
            @Parameter(description = B_ID) @PathParam("id") String id,
            @Valid @BeanParam PageParameters pageParameters);

    @GET
    @Path("/{id}/artifacts/built")
    Page<Artifact> getBuiltArtifacts(
            @Parameter(description = B_ID) @PathParam("id") String id,
            @Valid @BeanParam PageParameters pageParameters);
}