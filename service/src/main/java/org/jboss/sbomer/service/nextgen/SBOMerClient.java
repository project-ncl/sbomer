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
package org.jboss.sbomer.service.nextgen;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.jboss.sbomer.service.rest.api.v1beta2.payloads.generation.GenerationsRequest;
import org.jboss.sbomer.service.rest.api.v1beta2.payloads.generation.GenerationsResponse;
import org.jboss.sbomer.service.rest.otel.SpanName;
import org.jboss.sbomer.service.rest.otel.Traced;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * A client for SBOMer REST API.
 */
@ApplicationScoped
// @ClientHeaderParam(name = "User-Agent", value = "SBOMer")
@RegisterRestClient(configKey = "sbomer")
@Path("/api/v1beta2")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface SBOMerClient {

    @Traced
    @SpanName("sbomer.generations.post")
    @POST
    @Path("/generations")
    GenerationsResponse requestGenerations(GenerationsRequest payload);
}
