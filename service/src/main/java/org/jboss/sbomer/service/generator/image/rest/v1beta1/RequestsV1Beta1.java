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
package org.jboss.sbomer.service.generator.image.rest.v1beta1;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.ExampleObject;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.sbomer.core.SchemaValidator.ValidationResult;
import org.jboss.sbomer.core.config.ConfigSchemaValidator;
import org.jboss.sbomer.core.dto.BaseSbomGenerationRequestRecord;
import org.jboss.sbomer.core.dto.v1beta1.V1Beta1SbomGenerationRequestRecord;
import org.jboss.sbomer.core.errors.ClientException;
import org.jboss.sbomer.core.errors.ErrorResponse;
import org.jboss.sbomer.core.errors.NotFoundException;
import org.jboss.sbomer.core.features.sbom.config.AdvisoryConfig;
import org.jboss.sbomer.core.features.sbom.config.Config;
import org.jboss.sbomer.core.features.sbom.config.DeliverableAnalysisConfig;
import org.jboss.sbomer.core.features.sbom.config.OperationConfig;
import org.jboss.sbomer.core.features.sbom.config.PncBuildConfig;
import org.jboss.sbomer.core.features.sbom.config.SyftImageConfig;
import org.jboss.sbomer.core.features.sbom.rest.Page;
import org.jboss.sbomer.core.features.sbom.utils.ObjectMapperProvider;
import org.jboss.sbomer.core.utils.PaginationParameters;
import org.jboss.sbomer.service.feature.sbom.mapper.V1Beta1Mapper;
import org.jboss.sbomer.service.feature.sbom.model.SbomGenerationRequest;
import org.jboss.sbomer.service.feature.sbom.service.AdvisoryService;
import org.jboss.sbomer.service.feature.sbom.service.SbomService;
import org.jboss.util.NotImplementedException;

import com.fasterxml.jackson.jakarta.rs.yaml.YAMLMediaTypes;

import jakarta.annotation.security.PermitAll;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
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
    V1Beta1Mapper mapper;

    @Inject
    SbomService sbomService;

    @Inject
    AdvisoryService advisoryService;

    @Inject
    ConfigSchemaValidator configSchemaValidator;

    @POST
    @Consumes({ MediaType.APPLICATION_JSON, YAMLMediaTypes.APPLICATION_JACKSON_YAML })
    @Operation(
            summary = "Request manifest generation for a supported content",
            description = "Schedules generation of a manifest. This is an asynchronous call. It does perform the generation behind the scenes.")
    @RequestBody(
            required = true,
            content = @Content(
                    schema = @Schema(
                            required = true,
                            discriminatorProperty = "type",
                            anyOf = { PncBuildConfig.class, SyftImageConfig.class }),
                    examples = { //
                            @ExampleObject(
                                    name = "PNC build with defaults",
                                    description = "Requests manifest generation for a PNC build with identifier: ARYT3LBXDVYAC using defaults",
                                    value = "{\"type\": \"pnc-build\", \"buildId\": \"ARYT3LBXDVYAC\"}"),
                            @ExampleObject(
                                    name = "PNC build with one product defined and a custom generator",
                                    description = "Requests manifest generation for a PNC build with identifier: ARYT3LBXDVYAC with custom generator parameters",
                                    value = "{\"type\": \"pnc-build\", \"buildId\": \"ARYT3LBXDVYAC\", \"products\":[{\"generator\":{\"type\":\"maven-domino\",\"args\":\"--warn-on-missing-scm --legacy-scm-locator\",\"version\":\"0.0.107\"}}]}"),
                            @ExampleObject(
                                    name = "PNC operation",
                                    description = "Requests manifest generation for a PNC operation with identifier: A5WL3DFZ3AIAA",
                                    value = "{\"type\": \"pnc-operation\", \"operationId\": \"A5WL3DFZ3AIAA\"}"),
                            @ExampleObject(
                                    name = "Errata Tool advisory",
                                    description = "Requests manifest generation for the 12345 advisory",
                                    value = "{\"type\": \"advisory\", \"advisoryId\": 12345}"),
                            @ExampleObject(
                                    name = "Container image",
                                    description = "Requests manifest generation for a container image with iodentifier: registry.access.redhat.com/ubi9/ubi-micro:9.4",
                                    value = "{\"type\": \"syft-image\", \"image\": \"registry.access.redhat.com/ubi9/ubi-micro:9.4\"}") //
                    }))
    @APIResponse(
            responseCode = "202",
            description = "Manifest generation successfully requested",
            content = @Content(schema = @Schema(implementation = V1Beta1SbomGenerationRequestRecord.class)))
    @APIResponse(
            responseCode = "500",
            description = "Internal server error",
            content = @Content(mediaType = MediaType.APPLICATION_JSON))
    public Response createFromConfig(Config config) throws Exception {
        if (config == null) {
            throw new ClientException("No config provided");
        }

        log.info("Validating config: {}", ObjectMapperProvider.json().writeValueAsString(config));

        ValidationResult validationResult = configSchemaValidator.validate(config);

        if (!validationResult.isValid()) {
            throw new ClientException("Invalid config", validationResult.getErrors());
        }

        log.info("Provided config is valid!");

        List<SbomGenerationRequest> requests = new ArrayList<>();

        if (config instanceof AdvisoryConfig advisoryConfig) {
            log.info("New Errata advisory request received");
            requests.addAll(advisoryService.generateFromAdvisory(advisoryConfig.getAdvisoryId()));
        } else if (config instanceof PncBuildConfig pncBuildConfig) {
            log.info("New PNC build request received");
            requests.add(sbomService.generateFromBuild(pncBuildConfig.getBuildId(), pncBuildConfig));
        } else if (config instanceof DeliverableAnalysisConfig analysisConfig) {
            log.info("New PNC analysis request received");
            requests.add(sbomService.generateNewOperation(analysisConfig));
        } else if (config instanceof SyftImageConfig syftImageConfig) {
            log.info("New container image request received");
            requests.add(sbomService.generateSyftImage(syftImageConfig.getName(), syftImageConfig));
        } else if (config instanceof OperationConfig) {
            log.info("New PNC operation request received");

            throw new NotImplementedException("Operation is not implemented yet");
        }

        return Response.accepted(mapper.requestsToRecords(requests)).build();
    }

    @GET
    @Path("/{id}")
    @Operation(
            summary = "Get specific manifest generation request",
            description = "Get generation request for the provided identifier or purl")
    @Parameter(
            name = "id",
            description = "Manifest generation request identifier",
            examples = { @ExampleObject(value = "88CA2291D4014C6", name = "Generation request identifier") })
    @APIResponse(
            responseCode = "200",
            description = "The generation request",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = V1Beta1SbomGenerationRequestRecord.class)))
    @APIResponse(
            responseCode = "400",
            description = "Malformed request",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = ErrorResponse.class)))
    @APIResponse(
            responseCode = "404",
            description = "Generation request could not be found",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = ErrorResponse.class)))
    @APIResponse(
            responseCode = "500",
            description = "Internal server error",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = ErrorResponse.class)))
    public V1Beta1SbomGenerationRequestRecord getGenerationRequestById(@PathParam("id") String generationRequestId) {
        SbomGenerationRequest generationRequest = SbomGenerationRequest.findById(generationRequestId); // NOSONAR

        if (generationRequest == null) {
            throw new NotFoundException("Generation request with id '{}' could not be found", generationRequestId);
        }

        return mapper.toRecord(generationRequest);
    }

    @GET
    @Operation(
            summary = "Search for manifest generation requests",
            description = "Paginated list of SBOM generation requests using RSQL advanced search.")
    @Parameter(
            name = "query",
            description = "A RSQL query to search the generation requests",
            examples = { @ExampleObject(
                    name = "Find all SBOM generation requests with provided identifier",
                    value = "identifier=eq=ABCDEFGHIJKLM") })
    @Parameter(
            name = "sort",
            description = "Optional RSQL sort",
            examples = { @ExampleObject(name = "Order generation requests by id in ascending order", value = "id=asc="),
                    @ExampleObject(
                            name = "Order generation requests by creation time in descending order",
                            value = "creationTime=desc=") })
    @APIResponse(
            responseCode = "200",
            description = "List of SBOM generation requests in the system for a specified RSQL query.",
            content = @Content(mediaType = MediaType.APPLICATION_JSON))
    @APIResponse(
            responseCode = "400",
            description = "Failed while parsing the provided RSQL string, please verify the correct syntax.",
            content = @Content(mediaType = MediaType.APPLICATION_JSON))
    @APIResponse(
            responseCode = "500",
            description = "Internal server error",
            content = @Content(mediaType = MediaType.APPLICATION_JSON))
    public Page<BaseSbomGenerationRequestRecord> searchGenerationRequests(
            @Valid @BeanParam PaginationParameters paginationParams,
            @QueryParam("query") String rsqlQuery,
            @DefaultValue("creationTime=desc=") @QueryParam("sort") String sort) {
        Page<SbomGenerationRequest> requests = sbomService.searchSbomRequestsByQueryPaginated(
                paginationParams.getPageIndex(),
                paginationParams.getPageSize(),
                rsqlQuery,
                sort);

        return mapper.requestsToBaseRecordPage(requests);
    }
}
