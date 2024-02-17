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
package org.jboss.sbomer.cli.test.integ.feature.sbom.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.sbomer.cli.feature.sbom.client.SBOMerClient;
import org.jboss.sbomer.cli.feature.sbom.model.Sbom;
import org.jboss.sbomer.cli.feature.sbom.model.SbomGenerationRequest;
import org.jboss.sbomer.core.errors.ErrorResponse;
import org.jboss.sbomer.core.features.sbom.rest.Page;
import org.jboss.sbomer.core.utils.PaginationParameters;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

@QuarkusTest
@QuarkusTestResource(ServiceWireMock.class)
public class SBOMerClientTestIT {

    @Inject
    @RestClient
    SBOMerClient client;

    @Test
    void testGetValidSbom() {
        Response response = client.getById("123", "123");
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        Sbom sbom = response.readEntity(Sbom.class);
        assertNotNull(sbom);
        assertNotNull(sbom.getSbom());
        assertEquals("123", sbom.getId());
        assertEquals("QUARKUS", sbom.getBuildId());
        assertNotNull(sbom.getGenerationRequest());
        assertEquals("AABBCC", sbom.getGenerationRequest().getId());
        assertEquals("QUARKUS", sbom.getGenerationRequest().getBuildId());

    }

    @Test
    void testNotFoundSbom() {
        Response response = client.getById("1234", "1234");
        assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
        ErrorResponse errorResponse = (ErrorResponse) response.readEntity(ErrorResponse.class);
        assertEquals("Not Found", errorResponse.getMessage());
    }

    @Test
    void testSearchSbom() {
        PaginationParameters pagParams = new PaginationParameters();
        pagParams.setPageIndex(0);
        pagParams.setPageSize(1);
        String rsqlQuery = "id==123";
        Response response = client.searchSboms("123", pagParams, rsqlQuery, null);
        String json = response.readEntity(String.class);
        ObjectMapper objectMapper = new ObjectMapper();
        TypeReference<Page<Sbom>> typeReference = new TypeReference<Page<Sbom>>() {
        };
        try {
            Page<Sbom> sboms = objectMapper.readValue(json, typeReference);
            assertNotNull(sboms);
            assertEquals("123", sboms.getContent().iterator().next().getId());
            assertEquals("QUARKUS", sboms.getContent().iterator().next().getBuildId());
        } catch (JsonMappingException e) {
            fail(e.getMessage());
        } catch (JsonProcessingException e) {
            fail(e.getMessage());
        }
    }

    @Test
    void testSearchSbomWithSuccessfulGenerations() {
        PaginationParameters pagParams = new PaginationParameters();
        pagParams.setPageIndex(0);
        pagParams.setPageSize(20);

        String rsqlQuery = "buildId=eq=QUARKUS;generationRequest.buildId=eq=QUARKUS;generationRequest.status=eq=FINISHED;generationRequest.result=eq=SUCCESS";
        String sortQuery = "creationTime=desc=";
        Response response = client.searchSboms("QUARKUS", pagParams, rsqlQuery, sortQuery);
        String json = response.readEntity(String.class);
        ObjectMapper objectMapper = new ObjectMapper();
        TypeReference<Page<Sbom>> typeReference = new TypeReference<Page<Sbom>>() {
        };
        try {
            Page<Sbom> sboms = objectMapper.readValue(json, typeReference);
            assertNotNull(sboms);
            assertEquals("123", sboms.getContent().iterator().next().getId());
            assertEquals("QUARKUS", sboms.getContent().iterator().next().getBuildId());
            assertEquals("QUARKUS", sboms.getContent().iterator().next().getGenerationRequest().getBuildId());

        } catch (JsonMappingException e) {
            fail(e.getMessage());
        } catch (JsonProcessingException e) {
            fail(e.getMessage());
        }
    }

    @Test
    void testSearchSbomRequest() {
        PaginationParameters pagParams = new PaginationParameters();
        pagParams.setPageIndex(0);
        pagParams.setPageSize(1);
        String rsqlQuery = "id==AABBCC";
        String sortQuery = "creationTime=desc=";
        Response response = client.searchGenerationRequests("AABBCC", pagParams, rsqlQuery, sortQuery);
        String json = response.readEntity(String.class);
        ObjectMapper objectMapper = new ObjectMapper();
        TypeReference<Page<SbomGenerationRequest>> typeReference = new TypeReference<Page<SbomGenerationRequest>>() {
        };
        try {
            Page<SbomGenerationRequest> sbomRequests = objectMapper.readValue(json, typeReference);
            assertNotNull(sbomRequests);
            assertEquals("AABBCC", sbomRequests.getContent().iterator().next().getId());
            assertEquals("QUARKUS", sbomRequests.getContent().iterator().next().getBuildId());
        } catch (JsonMappingException e) {
            fail(e.getMessage());
        } catch (JsonProcessingException e) {
            fail(e.getMessage());
        }
    }
}