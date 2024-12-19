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
package org.jboss.sbomer.service.rest.api.v1beta1;

import java.util.List;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.ExampleObject;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.sbomer.core.dto.v1beta1.V1BaseBeta1RequestRecord;
import org.jboss.sbomer.core.dto.v1beta1.V1Beta1RequestRecord;
import org.jboss.sbomer.core.errors.ErrorResponse;
import org.jboss.sbomer.core.features.sbom.rest.Page;
import org.jboss.sbomer.core.utils.PaginationParameters;
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
import lombok.extern.slf4j.Slf4j;

@Path("/api/v1beta1/requests")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
@PermitAll
@Tag(name = "v1beta1")
@Slf4j
public class RequestsV1Beta1 {

    @Inject
    SbomService sbomService;

    @GET
    @Path("/{filter}")
    @Operation(
            summary = "Get request events with all manifests",
            description = "Get all request events matching the provided filter, with all the generated manifests")
    @Parameter(
            name = "filter",
            description = "The filter used to find the request events",
            examples = { @ExampleObject(value = "id=88CA2291D4014C6", name = "Filter a specific request event by id"),
                    @ExampleObject(
                            value = "errata-advisory=139787",
                            name = "Filter requests of events generated for the specified Errata advisory"),
                    @ExampleObject(
                            value = "image=registry.com/sbomerimage@sha256:c5e403466b5f7f2e7596840fe82b890f65f0d61c59dbed10a362249f085f9ebb",
                            name = "Filter requests of events generated for the specified container image"),
                    @ExampleObject(
                            value = "pnc-build=ARYT3LBXDVYAC",
                            name = "Filter requests of events generated for the specified PNC build"),
                    @ExampleObject(
                            value = "pnc-analysis=1234",
                            name = "Filter requests of events generated for the PNC analysis on the for the specified milestone"),
                    @ExampleObject(
                            value = "pnc-operation=BDQXCNRZJYYAA",
                            name = "Filter requests of events generated for the specified PNC operation"),
                    @ExampleObject(
                            value = "release.errata_id=1234",
                            name = "Filter requests of events generated for the specified Errata release by advisory id "),
                    @ExampleObject(
                            value = "release.errata_fullname=ERRATA",
                            name = "Filter requests of events generated for the specified Errata release by advisory name") })
    @APIResponse(
            responseCode = "200",
            description = "The request event with all the manifest generated",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = V1Beta1RequestRecord.class)))
    @APIResponse(
            responseCode = "400",
            description = "Malformed request",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = ErrorResponse.class)))
    @APIResponse(
            responseCode = "404",
            description = "Request event could not be found",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = ErrorResponse.class)))
    @APIResponse(
            responseCode = "500",
            description = "Internal server error",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = ErrorResponse.class)))
    public List<V1Beta1RequestRecord> getRequestEventById(@PathParam("filter") String filter) {
        return sbomService.searchAggregatedResultsNatively(filter);
    }

    @GET
    @Operation(
            summary = "Search request events",
            description = "Paginated list of request events using RSQL advanced search.")
    @Parameter(
            name = "query",
            description = "A RSQL query to search the request events",
            examples = {
                    @ExampleObject(
                            name = "Find all request events with provided identifier",
                            value = "id=eq=88CA2291D4014C6"),
                    @ExampleObject(name = "Find all request received from REST events", value = "eventType=eq=REST"),
                    @ExampleObject(name = "Find all request received from UMB events", value = "eventType=eq=UMB") })
    @Parameter(
            name = "sort",
            description = "Optional RSQL sort",
            examples = { @ExampleObject(name = "Order request events by id in ascending order", value = "id=asc="),
                    @ExampleObject(
                            name = "Order request events by receival time in descending order",
                            value = "receivalTime=desc=") })
    @APIResponse(
            responseCode = "200",
            description = "Paginated list of request events in the system for a specified RSQL query.",
            content = @Content(mediaType = MediaType.APPLICATION_JSON))
    @APIResponse(
            responseCode = "400",
            description = "Failed while parsing the provided RSQL string, please verify the correct syntax.",
            content = @Content(mediaType = MediaType.APPLICATION_JSON))
    @APIResponse(
            responseCode = "500",
            description = "Internal server error",
            content = @Content(mediaType = MediaType.APPLICATION_JSON))
    public Page<V1BaseBeta1RequestRecord> searchRequestEvents(
            @Valid @BeanParam PaginationParameters paginationParams,
            @QueryParam("query") String rsqlQuery,
            @DefaultValue("receivalTime=desc=") @QueryParam("sort") String sort) {

        Page<V1BaseBeta1RequestRecord> requests = sbomService.searchRequestRecordsByQueryPaginated(
                paginationParams.getPageIndex(),
                paginationParams.getPageSize(),
                rsqlQuery,
                sort);

        return requests;
    }

}
