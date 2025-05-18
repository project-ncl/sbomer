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
package org.jboss.sbomer.service.rest.api.v1beta2;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.sbomer.core.errors.ClientException;
import org.jboss.sbomer.core.errors.ErrorResponse;
import org.jboss.sbomer.core.features.sbom.enums.GenerationRequestType;
import org.jboss.sbomer.core.features.sbom.enums.TaskStatus;
import org.jboss.sbomer.core.features.sbom.enums.TaskType;
import org.jboss.sbomer.core.utils.PaginationParameters;
import org.jboss.sbomer.service.feature.sbom.k8s.model.GenerationStatus;
import org.jboss.sbomer.service.feature.sbom.model.RandomStringIdGenerator;
import org.jboss.sbomer.service.feature.sbom.model.v1beta2.Generation;
import org.jboss.sbomer.service.feature.sbom.model.v1beta2.Task;
import org.jboss.sbomer.service.feature.sbom.service.SbomService;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.jakarta.rs.yaml.YAMLMediaTypes;

import jakarta.annotation.security.PermitAll;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;

@Path("/api/v1beta2/tasks")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
@PermitAll
@Tag(name = "v1beta2")
@Slf4j
public class TasksV1Beta2 {

    @Inject
    SbomService sbomService;

    @POST
    @Transactional
    @Consumes({ MediaType.APPLICATION_JSON, YAMLMediaTypes.APPLICATION_JACKSON_YAML })
    @Operation(
            summary = "Create a new generation task",
            description = "Creates a new task within the system to perform a generation")
    @RequestBody(
            content = @Content(
                    schema = @Schema(required = true, discriminatorProperty = "type", anyOf = {}), // TODO: populate it
                    examples = {})) // TODO: populate it
    @APIResponse(
            responseCode = "202",
            description = "Task successfully created",
            content = @Content(schema = @Schema(implementation = Map.class))) // TODO: populate it
    @APIResponse(
            responseCode = "400",
            description = "Failed while processing the request, please verify the provided content",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = ErrorResponse.class)))
    @APIResponse(
            responseCode = "500",
            description = "Internal server error",
            content = @Content(mediaType = MediaType.APPLICATION_JSON))
    public Response create(JsonNode config, @Context ContainerRequestContext requestContext) throws Exception {
        if (config == null) {
            throw new ClientException("No config provided");
        }

        Generation generation = Generation.builder()
                .withId(RandomStringIdGenerator.generate())
                .withIdentifier("DUMMY")
                .withStatus(GenerationStatus.NEW)
                .withType(GenerationRequestType.BUILD.toName())
                .build();

        Task task = Task.builder()
                .withId(RandomStringIdGenerator.generate())
                .withCreated(Instant.now())
                .withStatus(TaskStatus.NEW)
                .withEventType(TaskType.REST.toName()) // TODO: we don't have an enum here anymore
                .withEvent(JsonNodeFactory.instance.objectNode()) // TODO: dummy
                .withGenerations(List.of(generation))
                .build()
                .save();

        return Response.accepted(task).build();
    }

    @GET
    @Operation(summary = "Search tasks", description = "Paginated list of tasks")
    @APIResponse(
            responseCode = "200",
            description = "Paginated list of tasks found",
            content = @Content(mediaType = MediaType.APPLICATION_JSON))
    @APIResponse(
            responseCode = "500",
            description = "Internal server error",
            content = @Content(mediaType = MediaType.APPLICATION_JSON))
    public Response search( // TODO: USE paging
            @Valid @BeanParam PaginationParameters paginationParams,
            @QueryParam("query") String rsqlQuery,
            @DefaultValue("creationTime=desc=") @QueryParam("sort") String sort) {

        List<Task> tasks = Task.findAll().list();

        return Response.ok(tasks).build();

    }
}
