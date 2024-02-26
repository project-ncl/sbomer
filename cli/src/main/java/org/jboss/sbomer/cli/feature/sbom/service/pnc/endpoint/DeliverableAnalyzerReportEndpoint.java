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
import org.jboss.pnc.dto.DeliverableAnalyzerReport;
import org.jboss.pnc.dto.response.AnalyzedArtifact;

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

@Tag(name = "Deliverable Analysis")
@Path("/deliverable-analyses")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface DeliverableAnalyzerReportEndpoint {

    String DEL_AN_ID = "Id of the Deliverable Analysis Report";

    @GET
    @Path("/{id}")
    DeliverableAnalyzerReport getSpecific(@Parameter(description = DEL_AN_ID) @PathParam("id") String id);

    @Path("/{id}/analyzed-artifacts")
    @GET
    Page<AnalyzedArtifact> getAnalyzedArtifacts(
            @Parameter(description = DEL_AN_ID) @PathParam("id") String id,
            @Valid @BeanParam PageParameters pageParameters);
}
