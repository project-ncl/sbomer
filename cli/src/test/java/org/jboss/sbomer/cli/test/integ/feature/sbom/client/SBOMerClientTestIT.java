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
package org.jboss.sbomer.cli.test.integ.feature.sbom.client;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.List;
import java.util.Map;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.sbomer.cli.feature.sbom.client.SBOMerClient;
import org.jboss.sbomer.cli.test.integ.feature.sbom.client.SBOMerClientTestIT.CustomSbomerProfile;
import org.jboss.sbomer.core.dto.v1alpha3.SbomGenerationRequestRecord;
import org.jboss.sbomer.core.dto.v1alpha3.SbomRecord;
import org.jboss.sbomer.core.errors.ErrorResponse;
import org.jboss.sbomer.core.features.sbom.rest.Page;
import org.jboss.sbomer.core.utils.PaginationParameters;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;

import io.quarkus.test.common.WithTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import lombok.extern.slf4j.Slf4j;

@QuarkusTest
@TestProfile(CustomSbomerProfile.class)
@WithTestResource(ServiceWireMock.class)
@Slf4j
class SBOMerClientTestIT {

    @Inject
    @RestClient
    SBOMerClient client;

    @InjectWireMock
    WireMockServer wireMockServer;

    public static class CustomSbomerProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("quarkus.rest-client.read-timeout", "1600");
        }
    }

    @Test
    void testGetValidSbom() {
        Response response = client.getById("123", "123");
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        SbomRecord sbom = response.readEntity(SbomRecord.class);
        assertNotNull(sbom);
        assertNotNull(sbom.sbom());
        assertEquals("123", sbom.id());
        assertEquals("QUARKUS", sbom.identifier());
        assertNotNull(sbom.generationRequest());
        assertEquals("AABBCC", sbom.generationRequest().id());
        assertEquals("QUARKUS", sbom.generationRequest().identifier());
    }

    @Test
    void testNotFoundSbom() {
        Response response = client.getById("1234", "1234");
        assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
        ErrorResponse errorResponse = response.readEntity(ErrorResponse.class);
        assertEquals("Not Found", errorResponse.getMessage());
    }

    @Test
    void testSearchSbom() {
        PaginationParameters pagParams = new PaginationParameters();
        pagParams.setPageIndex(0);
        pagParams.setPageSize(1);
        String rsqlQuery = "id==123";
        try (Response response = client.searchSboms("123", pagParams, rsqlQuery, null)) {
            String json = response.readEntity(String.class);
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.registerModule(new JavaTimeModule());
            TypeReference<Page<SbomRecord>> typeReference = new TypeReference<>() {
            };
            try {
                Page<SbomRecord> sboms = objectMapper.readValue(json, typeReference);
                assertNotNull(sboms);
                assertEquals("123", sboms.getContent().iterator().next().id());
                assertEquals("QUARKUS", sboms.getContent().iterator().next().identifier());
            } catch (JsonProcessingException e) {
                fail(e.getMessage());
            }
        }
    }

    @Test
    void testSearchSbomWithSuccessfulGenerations() {
        PaginationParameters pagParams = new PaginationParameters();
        pagParams.setPageIndex(0);
        pagParams.setPageSize(20);

        String rsqlQuery = "identifier=eq=QUARKUS;generationRequest.identifier=eq=QUARKUS;generationRequest.status=eq=FINISHED;generationRequest.result=eq=SUCCESS;generationRequest.type=eq=BUILD";
        String sortQuery = "creationTime=desc=";
        try (Response response = client.searchSboms("QUARKUS", pagParams, rsqlQuery, sortQuery)) {
            String json = response.readEntity(String.class);
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.registerModule(new JavaTimeModule());
            TypeReference<Page<SbomRecord>> typeReference = new TypeReference<>() {
            };
            try {
                Page<SbomRecord> sboms = objectMapper.readValue(json, typeReference);
                assertNotNull(sboms);
                assertEquals("123", sboms.getContent().iterator().next().id());
                assertEquals("QUARKUS", sboms.getContent().iterator().next().identifier());
                assertEquals("QUARKUS", sboms.getContent().iterator().next().generationRequest().identifier());

            } catch (JsonProcessingException e) {
                fail(e.getMessage());
            }
        }
    }

    @Test
    void testSearchSbomRequest() {
        testSearchSbomRequestParamerterised("AABBCC", "id==AABBCC");
    }

    private void testSearchSbomRequestParamerterised(String id, String rsqlQuery) {
        PaginationParameters pagParams = new PaginationParameters();
        pagParams.setPageIndex(0);
        pagParams.setPageSize(1);
        String sortQuery = "creationTime=desc=";
        try (Response response = client.searchGenerationRequests(id, pagParams, rsqlQuery, sortQuery)) {
            String json = response.readEntity(String.class);
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.registerModule(new JavaTimeModule());
            TypeReference<Page<SbomGenerationRequestRecord>> typeReference = new TypeReference<>() {
            };
            try {
                Page<SbomGenerationRequestRecord> sbomRequests = objectMapper.readValue(json, typeReference);
                assertNotNull(sbomRequests);
                assertEquals(id, sbomRequests.getContent().iterator().next().id());
                assertEquals("QUARKUS", sbomRequests.getContent().iterator().next().identifier());
            } catch (JsonProcessingException e) {
                fail(e.getMessage());
            }
        }
    }

    @Test
    void testNetworkConnectionTimeout() {

        // Check for ultimate success first
        testSearchSbomRequestParamerterised("AABBCCDD", "id==AABBCCDD");

        // Check what actually happened after
        // Print the requests for visual confirmation
        List<ServeEvent> servs = wireMockServer.getAllServeEvents();
        for (ServeEvent se : servs) {
            log.info(
                    """
                            Request URL: {},
                            Method: {},
                            Request ts: {},
                            Matched Stub Id: {}
                            Matched Stub Name: {}
                            """,
                    se.getRequest().getUrl(),
                    se.getRequest().getMethod(),
                    se.getRequest().getLoggedDate().toInstant().toEpochMilli(),
                    se.getStubMapping().getId(),
                    se.getStubMapping().getNewScenarioState());
        }

        // One timeout, one failure and one success, all should be retried
        wireMockServer.verify(
                3,
                getRequestedFor(
                        urlEqualTo(
                                "/api/v1beta1/generations?pageIndex=0&pageSize=1&query=id%3D%3DAABBCCDD&sort=creationTime%3Ddesc%3D")));
    }
}
