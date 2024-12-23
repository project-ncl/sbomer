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
package org.jboss.sbomer.service.test.integ.feature.sbom;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;

import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;
import org.jboss.sbomer.core.dto.BaseSbomRecord;
import org.jboss.sbomer.core.features.sbom.enums.GenerationRequestType;
import org.jboss.sbomer.core.features.sbom.rest.Page;
import org.jboss.sbomer.core.test.TestResources;
import org.jboss.sbomer.service.feature.sbom.model.Sbom;
import org.jboss.sbomer.service.feature.sbom.model.SbomGenerationRequest;
import org.jboss.sbomer.service.feature.sbom.service.SbomService;
import org.jboss.sbomer.service.test.utils.umb.TestUmbProfile;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.junit.mockito.InjectSpy;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;

@QuarkusTest
@TestProfile(TestUmbProfile.class)
class SBOMResourceRSQTest {

    @InjectSpy
    SbomService sbomService;

    @Nested
    class v1alpha3 {
        static String API_PATH = "/api/v1alpha3";

        @Test
        void testGetSbomContentWithSearch() {
            given().when()
                    .contentType(ContentType.JSON)
                    .request("GET", API_PATH + "/sboms")
                    .then()
                    .statusCode(200)
                    .body("pageIndex", CoreMatchers.equalTo(0))
                    .and()
                    .body("pageSize", CoreMatchers.equalTo(50))
                    .and()
                    .body("totalPages", CoreMatchers.equalTo(1))
                    .and()
                    .body("totalHits", CoreMatchers.equalTo(2))
                    .and()
                    .body("content.id", CoreMatchers.hasItem("416640206274228224"))
                    .and()
                    .body("content[0].identifier", CoreMatchers.is("ARYT3LBXDVYAC"))
                    .and()
                    .body("content[0].sbom", CoreMatchers.is(CoreMatchers.nullValue()));
        }

        @Nested
        class GetByPurl {

            static String PURL = "pkg:maven/org.apache.logging.log4j/log4j@2.19.0.redhat-00001?type=pom";
            static String PURL_WITH_REPOSITORY_URL = "pkg:maven/org.apache.logging.log4j/log4j@2.19.0.redhat-00001?repository_url=https%3A%2F%2Fmaven.repository.redhat.com%2Fga%2F&type=pom";

            @Test
            void testFetchSbomByPurl() throws Exception {
                Mockito.when(sbomService.findByPurl(PURL)).thenReturn(createFirstSbom());

                given().when()
                        .contentType(ContentType.JSON)
                        .pathParam("purl", PURL)
                        .get(API_PATH + "/sboms/purl/{purl}")
                        .then()
                        .statusCode(200)
                        .and()
                        .body("id", CoreMatchers.is("12345"))
                        .and()
                        .body("identifier", CoreMatchers.is("AWI7P3EJ23YAA"))
                        .and()
                        .body("sbom", CoreMatchers.is(CoreMatchers.notNullValue()));
            }

            @Test
            void testFetchSbomBomByPurl() throws Exception {
                Mockito.when(sbomService.findByPurl(PURL)).thenReturn(createFirstSbom());

                given().when()
                        .contentType(ContentType.JSON)
                        .pathParam("purl", PURL)
                        .get(API_PATH + "/sboms/purl/{purl}/bom")
                        .then()
                        .statusCode(200)
                        .and()
                        .body("metadata.component.name", CoreMatchers.is("microprofile-graphql-parent"))
                        .and()
                        // This purl doesn't match PURL above, but this is expected because of
                        // the test data used
                        .body(
                                "metadata.component.purl",
                                CoreMatchers.is(
                                        "pkg:maven/org.eclipse.microprofile.graphql/microprofile-graphql-parent@1.1.0.redhat-00008?type=pom"));
            }

            @Test
            void testFetchSbomByPurlNotFound() {
                Mockito.when(sbomService.findByPurl(PURL)).thenReturn(null);

                given().when()
                        .contentType(ContentType.JSON)
                        .pathParam("purl", PURL)
                        .get(API_PATH + "/sboms/purl/{purl}")
                        .then()
                        .statusCode(404)
                        .body("errorId", CoreMatchers.isA(String.class))
                        .body("error", CoreMatchers.is("Not Found"))
                        .body(
                                "message",
                                CoreMatchers.is(
                                        "Manifest with provided identifier: 'pkg:maven/org.apache.logging.log4j/log4j@2.19.0.redhat-00001?type=pom' couldn't be found"))
                        .body("$", Matchers.not(Matchers.hasKey("errors")));
            }

            @Test
            void testFetchSbomByPurlWithAllowedQualifier() throws Exception {

                Mockito.when(sbomService.findByPurl(PURL_WITH_REPOSITORY_URL)).thenReturn(createFirstSbom());

                given().when()
                        .contentType(ContentType.JSON)
                        .pathParam("purl", PURL_WITH_REPOSITORY_URL)
                        .get(API_PATH + "/sboms/purl/{purl}")
                        .then()
                        .statusCode(200)
                        .and()
                        .body("id", CoreMatchers.is("12345"))
                        .and()
                        .body("identifier", CoreMatchers.is("AWI7P3EJ23YAA"))
                        .and()
                        .body("sbom", CoreMatchers.is(CoreMatchers.notNullValue()));
            }
        }

    }

    @Test
    void testRSQLSearchPagination() throws Exception {
        // One page, one result
        int pageIndex = 0;
        int pageSizeLarge = 50;
        Page<BaseSbomRecord> singlePagedOneSbom = initializeOneResultRecordPaginated(pageIndex, pageSizeLarge);
        Mockito.when(
                sbomService.searchSbomRecordsByQueryPaginated(
                        pageIndex,
                        pageSizeLarge,
                        "identifier=eq=AWI7P3EJ23YAA",
                        "creationTime=desc="))
                .thenReturn(singlePagedOneSbom);

        given().when()
                .contentType(ContentType.JSON)
                .request(
                        "GET",
                        "/api/v1alpha3/sboms?pageIndex=" + pageIndex + "&pageSize=" + pageSizeLarge
                                + "&query=identifier=eq=AWI7P3EJ23YAA")
                .then()
                .statusCode(200)
                .body("pageIndex", CoreMatchers.equalTo(pageIndex))
                .and()
                .body("pageSize", CoreMatchers.equalTo(pageSizeLarge))
                .and()
                .body("totalPages", CoreMatchers.equalTo(1))
                .and()
                .body("totalHits", CoreMatchers.equalTo(1))
                .and()
                .body("content.id", CoreMatchers.hasItem("12345"))
                .and()
                .body("content.identifier", CoreMatchers.hasItem("AWI7P3EJ23YAA"));

        // One page, two results
        Page<BaseSbomRecord> singlePagedTwoSboms = initializeTwoResultsRecordPaginated(pageIndex, pageSizeLarge);
        Mockito.when(
                sbomService.searchSbomRecordsByQueryPaginated(
                        pageIndex,
                        pageSizeLarge,
                        "identifier=eq=AWI7P3EJ23YAA",
                        "creationTime=desc="))
                .thenReturn(singlePagedTwoSboms);

        given().when()
                .contentType(ContentType.JSON)
                .request(
                        "GET",
                        "/api/v1alpha3/sboms?pageIndex=" + pageIndex + "&pageSize=" + pageSizeLarge
                                + "&query=identifier=eq=AWI7P3EJ23YAA")
                .then()
                .statusCode(200)
                .body("pageIndex", CoreMatchers.equalTo(pageIndex))
                .and()
                .body("pageSize", CoreMatchers.equalTo(pageSizeLarge))
                .and()
                .body("totalPages", CoreMatchers.equalTo(1))
                .and()
                .body("totalHits", CoreMatchers.equalTo(2))
                .and()
                .body("content.id", CoreMatchers.hasItem("12345"))
                .and()
                .body("content.id", CoreMatchers.hasItem("54321"))
                .and()
                .body("content.identifier", CoreMatchers.hasItem("AWI7P3EJ23YAA"));

        // Two pages, two results
        int pageSizeTiny = 1;

        Page<BaseSbomRecord> doublePagedTwoSboms = initializeTwoResultsRecordPaginated(pageIndex, pageSizeTiny);
        Mockito.when(
                sbomService.searchSbomRecordsByQueryPaginated(
                        pageIndex,
                        pageSizeTiny,
                        "identifier=eq=AWI7P3EJ23YAA",
                        "creationTime=desc="))
                .thenReturn(doublePagedTwoSboms);

        given().when()
                .contentType(ContentType.JSON)
                .request(
                        "GET",
                        "/api/v1alpha3/sboms?pageIndex=" + pageIndex + "&pageSize=" + pageSizeTiny
                                + "&query=identifier=eq=AWI7P3EJ23YAA")
                .then()
                .statusCode(200)
                .body("pageIndex", CoreMatchers.equalTo(pageIndex))
                .and()
                .body("pageSize", CoreMatchers.equalTo(pageSizeTiny))
                .and()
                .body("totalPages", CoreMatchers.equalTo(2))
                .and()
                .body("totalHits", CoreMatchers.equalTo(2))
                .and()
                .body("content.id", CoreMatchers.hasItem("12345"))
                .and()
                .body("content.id", CoreMatchers.hasItem("54321"))
                .and()
                .body("content.identifier", CoreMatchers.hasItem("AWI7P3EJ23YAA"));
    }

    @Test
    void testRSQLSearchV1Alpha3() throws Exception {
        int pageIndex = 0;
        int pageSize = 50;

        Page<BaseSbomRecord> pagedSbomRecord = initializeOneResultRecordPaginated(pageIndex, pageSize);

        Mockito.when(
                sbomService.searchSbomRecordsByQueryPaginated(pageIndex, pageSize, "id==12345", "creationTime=desc="))
                .thenReturn(pagedSbomRecord);
        Mockito.when(
                sbomService.searchSbomRecordsByQueryPaginated(
                        pageIndex,
                        pageSize,
                        "identifier==AWI7P3EJ23YAA",
                        "creationTime=desc="))
                .thenReturn(pagedSbomRecord);
        Mockito.when(
                sbomService.searchSbomRecordsByQueryPaginated(
                        pageIndex,
                        pageSize,
                        "identifier=eq=AWI7P3EJ23YAA",
                        "creationTime=desc="))
                .thenReturn(pagedSbomRecord);
        Mockito.when(
                sbomService.searchSbomRecordsByQueryPaginated(
                        pageIndex,
                        pageSize,
                        "rootPurl=='pkg:maven/org.apache.logging.log4j/log4j@2.19.0.redhat-00001?type=pom'",
                        "creationTime=desc="))
                .thenReturn(pagedSbomRecord);
        Mockito.when(
                sbomService.searchSbomRecordsByQueryPaginated(
                        pageIndex,
                        pageSize,
                        "statusMessage=='all went well'",
                        "creationTime=desc="))
                .thenReturn(pagedSbomRecord);
        Mockito.when(
                sbomService.searchSbomRecordsByQueryPaginated(
                        pageIndex,
                        pageSize,
                        "statusMessage=='all*'",
                        "creationTime=desc="))
                .thenReturn(pagedSbomRecord);
        Mockito.when(
                sbomService.searchSbomRecordsByQueryPaginated(
                        pageIndex,
                        pageSize,
                        "statusMessage=='*went*'",
                        "creationTime=desc="))
                .thenReturn(pagedSbomRecord);

        given().when()
                .contentType(ContentType.JSON)
                .request(
                        "GET",
                        "/api/v1alpha3/sboms?pageIndex=" + pageIndex + "&pageSize=" + pageSize + "&query=id==12345")
                .then()
                .statusCode(200)
                .body("content.id", CoreMatchers.hasItem("12345"))
                .and()
                .body("content.identifier", CoreMatchers.hasItem("AWI7P3EJ23YAA"));

        given().when()
                .contentType(ContentType.JSON)
                .request(
                        "GET",
                        "/api/v1alpha3/sboms?pageIndex=" + pageIndex + "&pageSize=" + pageSize
                                + "&query=identifier==AWI7P3EJ23YAA")
                .then()
                .statusCode(200)
                .body("content.id", CoreMatchers.hasItem("12345"))
                .and()
                .body("content.identifier", CoreMatchers.hasItem("AWI7P3EJ23YAA"));

        given().when()
                .contentType(ContentType.JSON)
                .request(
                        "GET",
                        "/api/v1alpha3/sboms?pageIndex=" + pageIndex + "&pageSize=" + pageSize
                                + "&query=identifier=eq=AWI7P3EJ23YAA")
                .then()
                .statusCode(200)
                .body("content.id", CoreMatchers.hasItem("12345"))
                .and()
                .body("content.identifier", CoreMatchers.hasItem("AWI7P3EJ23YAA"));

        given().when()
                .contentType(ContentType.JSON)
                .request(
                        "GET",
                        "/api/v1alpha3/sboms?pageIndex=" + pageIndex + "&pageSize=" + pageSize
                                + "&query=rootPurl=='pkg:maven/org.apache.logging.log4j/log4j@2.19.0.redhat-00001?type=pom'")
                .then()
                .statusCode(200)
                .body("content.id", CoreMatchers.hasItem("12345"))
                .and()
                .body("content.identifier", CoreMatchers.hasItem("AWI7P3EJ23YAA"));

        given().when()
                .contentType(ContentType.JSON)
                .request(
                        "GET",
                        "/api/v1alpha3/sboms?pageIndex=" + pageIndex + "&pageSize=" + pageSize
                                + "&query=statusMessage=='all went well'")
                .then()
                .statusCode(200)
                .body("content.id", CoreMatchers.hasItem("12345"))
                .and()
                .body("content.identifier", CoreMatchers.hasItem("AWI7P3EJ23YAA"));

        given().when()
                .contentType(ContentType.JSON)
                .request(
                        "GET",
                        "/api/v1alpha3/sboms?pageIndex=" + pageIndex + "&pageSize=" + pageSize
                                + "&query=statusMessage=='all*'")
                .then()
                .statusCode(200)
                .body("content.id", CoreMatchers.hasItem("12345"))
                .and()
                .body("content.identifier", CoreMatchers.hasItem("AWI7P3EJ23YAA"));

        given().when()
                .contentType(ContentType.JSON)
                .request(
                        "GET",
                        "/api/v1alpha3/sboms?pageIndex=" + pageIndex + "&pageSize=" + pageSize
                                + "&query=statusMessage=='*went*'")
                .then()
                .statusCode(200)
                .body("content.id", CoreMatchers.hasItem("12345"))
                .and()
                .body("content.identifier", CoreMatchers.hasItem("AWI7P3EJ23YAA"));
    }

    @Test
    void testRSQLSearchNotNullValues() throws Exception {
        int pageIndex = 0;
        int pageSize = 50;
        Page<BaseSbomRecord> pagedSboms = initializeTwoResultsRecordPaginated(pageIndex, pageSize);

        Mockito.when(
                sbomService.searchSbomRecordsByQueryPaginated(
                        pageIndex,
                        pageSize,
                        "rootPurl=isnull=false",
                        "creationTime=desc="))
                .thenReturn(pagedSboms);

        given().when()
                .contentType(ContentType.JSON)
                .request(
                        "GET",
                        "/api/v1alpha3/sboms?pageIndex=" + pageIndex + "&pageSize=" + pageSize
                                + "&query=rootPurl=isnull=false")
                .then()
                .statusCode(200)
                .body("content.id", CoreMatchers.hasItem("12345"))
                .and()
                .body("content.identifier", CoreMatchers.hasItem("AWI7P3EJ23YAA"));
    }

    @Test
    void testRSQLSearchNullValues() throws Exception {
        int pageIndex = 0;
        int pageSize = 50;
        Page<BaseSbomRecord> pagedSboms = initializeTwoResultsRecordPaginated(pageIndex, pageSize);

        Mockito.when(
                sbomService.searchSbomRecordsByQueryPaginated(
                        pageIndex,
                        pageSize,
                        "rootPurl=isnull=true",
                        "creationTime=desc="))
                .thenReturn(pagedSboms);

        given().when()
                .contentType(ContentType.JSON)
                .request(
                        "GET",
                        "/api/v1alpha3/sboms?pageIndex=" + pageIndex + "&pageSize=" + pageSize
                                + "&query=rootPurl=isnull=true")
                .then()
                .statusCode(200)
                .body("content.id", CoreMatchers.hasItem("12345"))
                .and()
                .body("content.identifier", CoreMatchers.hasItem("AWI7P3EJ23YAA"));

    }

    @Test
    void testRSQLSearchNotAllowedProperty() {
        given().when()
                .contentType(ContentType.JSON)
                .request("GET", "/api/v1alpha3/sboms?query=sbom==null")
                .then()
                .statusCode(400)
                .body("resource", CoreMatchers.is("/api/v1alpha3/sboms"))
                .body("errorId", CoreMatchers.isA(String.class))
                .body("error", CoreMatchers.is("Bad Request"))
                .body(
                        "message",
                        CoreMatchers.is(
                                "Invalid arguments provided: RSQL on field Sbom.sbom with type JsonNode is not supported!"))
                .body("$", Matchers.not(Matchers.hasKey("errors")));
    }

    @Test
    void testRSQLSearchAndLogicalNode() throws Exception {

        int pageIndex = 0;
        int pageSize = 50;

        Page<BaseSbomRecord> oneContentPage = initializeOneResultRecordPaginated(1, 1);
        Page<BaseSbomRecord> twoContentPage = initializeTwoResultsRecordPaginated(1, 2);

        Mockito.when(
                sbomService.searchSbomRecordsByQueryPaginated(
                        pageIndex,
                        pageSize,
                        "identifier=eq=AWI7P3EJ23YAA",
                        "creationTime=desc="))
                .thenReturn(oneContentPage);
        Mockito.when(
                sbomService.searchSbomRecordsByQueryPaginated(
                        pageIndex,
                        pageSize,
                        "rootPurl=eq='pkg:maven/org.apache.logging.log4j/log4j@2.19.0.redhat-00001?type=pom'",
                        "creationTime=desc="))
                .thenReturn(oneContentPage);
        Mockito.when(
                sbomService.searchSbomRecordsByQueryPaginated(
                        pageIndex,
                        pageSize,
                        "identifier=eq=AWI7P3EJ23YAA;rootPurl=eq='pkg:maven/org.apache.logging.log4j/log4j@2.19.0.redhat-00001?type=pom'",
                        "creationTime=desc="))
                .thenReturn(oneContentPage);
        given().when()
                .contentType(ContentType.JSON)
                .request(
                        "GET",
                        "/api/v1alpha3/sboms?pageIndex=" + pageIndex + "&pageSize=" + pageSize
                                + "&query=identifier=eq=AWI7P3EJ23YAA;rootPurl=eq='pkg:maven/org.apache.logging.log4j/log4j@2.19.0.redhat-00001?type=pom'")
                .then()
                .statusCode(200)
                .body("content.id", CoreMatchers.hasItem("12345"))
                .and()
                .body("content.identifier", CoreMatchers.hasItem("AWI7P3EJ23YAA"));

        Mockito.when(
                sbomService.searchSbomRecordsByQueryPaginated(
                        pageIndex,
                        pageSize,
                        "identifier=eq=AWI7P3EJ23YAA,identifier=eq=AWI7P3EJ23YAB",
                        "id=asc="))
                .thenReturn(twoContentPage);

        given().when()
                .contentType(ContentType.JSON)
                .request(
                        "GET",
                        "/api/v1alpha3/sboms?pageIndex=" + pageIndex + "&pageSize=" + pageSize
                                + "&query=identifier=eq=AWI7P3EJ23YAA,identifier=eq=AWI7P3EJ23YAB&sort=id=asc=")
                .then()
                .statusCode(200)
                .body("content.id", CoreMatchers.hasItem("12345"))
                .and()
                .body("content.id", CoreMatchers.hasItem("54321"))
                .and()
                .body("content.identifier", CoreMatchers.hasItem("AWI7P3EJ23YAA"))
                .and()
                .body("content.identifier", CoreMatchers.hasItem("AWI7P3EJ23YAB"));

        given().when()
                .contentType(ContentType.JSON)
                .request(
                        "GET",
                        "/api/v1alpha3/sboms?pageIndex=" + pageIndex + "&pageSize=" + pageSize
                                + "&query=rootPurl=eq='pkg:maven/org.apache.logging.log4j/log4j@2.19.0.redhat-00001?type=pom'")
                .then()
                .statusCode(200)
                .body("content.id", CoreMatchers.hasItem("12345"))
                .and()
                .body("content.identifier", CoreMatchers.hasItem("AWI7P3EJ23YAA"));
    }

    @Test
    void testRSQLSearchAndLogicalNodeWithSorting() throws Exception {

        int pageIndex = 0;
        int pageSize = 20;

        Page<BaseSbomRecord> twoContentPage = initializeTwoResultsRecordPaginated(1, 2);

        Mockito.when(
                sbomService.searchSbomRecordsByQueryPaginated(
                        pageIndex,
                        pageSize,
                        "identifier=eq=AWI7P3EJ23YAA,identifier=eq=AWI7P3EJ23YAB",
                        "id=asc="))
                .thenReturn(twoContentPage);

        String response = given().when()
                .contentType(ContentType.JSON)
                .request(
                        "GET",
                        "/api/v1alpha3/sboms?pageIndex=" + pageIndex + "&pageSize=" + pageSize
                                + "&query=identifier=eq=AWI7P3EJ23YAA,identifier=eq=AWI7P3EJ23YAB&sort=id=asc=")
                .then()
                .statusCode(200)
                .extract()
                .asString();

        JsonPath jsonPath = new JsonPath(response);
        assertTrue(jsonPath.getString("content.id[0]").compareTo(jsonPath.getString("content.id[1]")) < 1);

        Mockito.when(
                sbomService.searchSbomRecordsByQueryPaginated(
                        pageIndex,
                        pageSize,
                        "identifier=eq=AWI7P3EJ23YAA,identifier=eq=AWI7P3EJ23YAB",
                        "creationTime=desc="))
                .thenReturn(initializeTwoResultsPaginatedInverted(1, 2));

        response = given().when()
                .contentType(ContentType.JSON)
                .request(
                        "GET",
                        "/api/v1alpha3/sboms?pageIndex=" + pageIndex + "&pageSize=" + pageSize
                                + "&query=identifier=eq=AWI7P3EJ23YAA,identifier=eq=AWI7P3EJ23YAB&sort=creationTime=desc=")
                .then()
                .statusCode(200)
                .extract()
                .asString();

        jsonPath = new JsonPath(response);
        Instant creationTime1 = Instant.parse(jsonPath.getString("content.creationTime[0]"));
        Instant creationTime2 = Instant.parse(jsonPath.getString("content.creationTime[1]"));

        assertTrue(creationTime1.isAfter(creationTime2));
    }

    private Page<BaseSbomRecord> initializeOneResultRecordPaginated(int pageIndex, int pageSize) throws Exception {
        int totalHits = 1;
        int totalPages = (int) Math.ceil((double) totalHits / (double) pageSize);
        return new Page<BaseSbomRecord>(
                pageIndex,
                pageSize,
                totalPages,
                totalHits,
                Arrays.asList(createFirstBaseSbomRecord()));
    }

    private Page<BaseSbomRecord> initializeTwoResultsRecordPaginated(int pageIndex, int pageSize) throws Exception {
        int totalHits = 2;
        int totalPages = (int) Math.ceil((double) totalHits / (double) pageSize);
        return new Page<BaseSbomRecord>(
                pageIndex,
                pageSize,
                totalPages,
                totalHits,
                Arrays.asList(createFirstBaseSbomRecord(), createSecondBaseSbomRecord()));
    }

    private Page<BaseSbomRecord> initializeTwoResultsPaginatedInverted(int pageIndex, int pageSize) throws Exception {
        int totalHits = 2;
        int totalPages = (int) Math.ceil((double) totalHits / (double) pageSize);
        return new Page<BaseSbomRecord>(
                pageIndex,
                pageSize,
                totalPages,
                totalHits,
                Arrays.asList(createSecondBaseSbomRecord(), createFirstBaseSbomRecord()));
    }

    private Sbom createFirstSbom() throws Exception {
        Sbom sbom = new Sbom();
        sbom.setId("12345");
        sbom.setIdentifier("AWI7P3EJ23YAA");
        sbom.setRootPurl("pkg:maven/org.apache.logging.log4j/log4j@2.19.0.redhat-00001?type=pom");
        sbom.setStatusMessage("all went well");
        sbom.setCreationTime(Instant.now().minus(Duration.ofDays(1)));

        String bomJson = TestResources.asString("sboms/complete_sbom.json");
        sbom.setSbom(new ObjectMapper().readTree(bomJson));

        SbomGenerationRequest generationRequest = new SbomGenerationRequest();
        generationRequest.setId("g12345");
        generationRequest.setIdentifier("gAWI7P3EJ23YAA");
        generationRequest.setCreationTime(Instant.now().minus(Duration.ofDays(1)));
        generationRequest.setType(GenerationRequestType.BUILD);

        sbom.setGenerationRequest(generationRequest);

        return sbom;
    }

    private BaseSbomRecord createFirstBaseSbomRecord() throws Exception {
        BaseSbomRecord sbom = new BaseSbomRecord(
                "12345",
                "AWI7P3EJ23YAA",
                "pkg:maven/org.apache.logging.log4j/log4j@2.19.0.redhat-00001?type=pom",
                Instant.now().minus(Duration.ofDays(1)),
                0,
                "all went well",
                "g12345",
                "gAWI7P3EJ23YAA",
                null,
                GenerationRequestType.BUILD,
                Instant.now().minus(Duration.ofDays(1)));

        return sbom;
    }

    private BaseSbomRecord createSecondBaseSbomRecord() throws Exception {
        BaseSbomRecord sbom = new BaseSbomRecord(
                "54321",
                "AWI7P3EJ23YAB",
                "pkg:maven/org.apache.logging.log4j/log4j@2.119.0.redhat-00002?type=pom",
                Instant.now(),
                0,
                "all went well, again",
                "g12345",
                "gAWI7P3EJ23YAB",
                null,
                GenerationRequestType.BUILD,
                Instant.now());

        return sbom;
    }

}
