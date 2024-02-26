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
import org.jboss.sbomer.core.features.sbom.rest.Page;
import org.jboss.sbomer.core.test.TestResources;
import org.jboss.sbomer.service.feature.sbom.model.Sbom;
import org.jboss.sbomer.service.feature.sbom.service.SbomService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectSpy;
import io.quarkus.test.kubernetes.client.WithKubernetesTestServer;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;

@QuarkusTest
@WithKubernetesTestServer
public class SBOMResourceRSQIT {

    @InjectSpy
    SbomService sbomService;

    @Nested
    class v1alpha1 {
        static String API_PATH = "/api/v1alpha1";

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
                    .body("content[0].sbom", CoreMatchers.is(CoreMatchers.notNullValue()));
        }
    }

    @Nested
    class v1alpha2 {
        static String API_PATH = "/api/v1alpha2";

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
                        // This purl doesn't match PURL above, but this is expected because of the test data used
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
                        .statusCode(404);
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
    public void testRSQLSearchPagination() throws Exception {
        // One page, one result
        int pageIndex = 0;
        int pageSizeLarge = 50;
        Page<Sbom> singlePagedOneSbom = initializeOneResultPaginated(pageIndex, pageSizeLarge);
        Mockito.when(
                sbomService.searchSbomsByQueryPaginated(
                        pageIndex,
                        pageSizeLarge,
                        "identifier=eq=AWI7P3EJ23YAA",
                        "creationTime=desc="))
                .thenReturn(singlePagedOneSbom);

        given().when()
                .contentType(ContentType.JSON)
                .request(
                        "GET",
                        "/api/v1alpha1/sboms?pageIndex=" + pageIndex + "&pageSize=" + pageSizeLarge
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
        Page<Sbom> singlePagedTwoSboms = initializeTwoResultsPaginated(pageIndex, pageSizeLarge);
        Mockito.when(
                sbomService.searchSbomsByQueryPaginated(
                        pageIndex,
                        pageSizeLarge,
                        "identifier=eq=AWI7P3EJ23YAA",
                        "creationTime=desc="))
                .thenReturn(singlePagedTwoSboms);

        given().when()
                .contentType(ContentType.JSON)
                .request(
                        "GET",
                        "/api/v1alpha1/sboms?pageIndex=" + pageIndex + "&pageSize=" + pageSizeLarge
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

        Page<Sbom> doublePagedTwoSboms = initializeTwoResultsPaginated(pageIndex, pageSizeTiny);
        Mockito.when(
                sbomService.searchSbomsByQueryPaginated(
                        pageIndex,
                        pageSizeTiny,
                        "identifier=eq=AWI7P3EJ23YAA",
                        "creationTime=desc="))
                .thenReturn(doublePagedTwoSboms);

        given().when()
                .contentType(ContentType.JSON)
                .request(
                        "GET",
                        "/api/v1alpha1/sboms?pageIndex=" + pageIndex + "&pageSize=" + pageSizeTiny
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
    public void testRSQLSearch() throws Exception {
        int pageIndex = 0;
        int pageSize = 50;

        Page<Sbom> pagedSboms = initializeOneResultPaginated(pageIndex, pageSize);

        Mockito.when(sbomService.searchSbomsByQueryPaginated(pageIndex, pageSize, "id==12345", "creationTime=desc="))
                .thenReturn(pagedSboms);
        Mockito.when(
                sbomService.searchSbomsByQueryPaginated(
                        pageIndex,
                        pageSize,
                        "identifier==AWI7P3EJ23YAA",
                        "creationTime=desc="))
                .thenReturn(pagedSboms);
        Mockito.when(
                sbomService.searchSbomsByQueryPaginated(
                        pageIndex,
                        pageSize,
                        "identifier=eq=AWI7P3EJ23YAA",
                        "creationTime=desc="))
                .thenReturn(pagedSboms);
        Mockito.when(
                sbomService.searchSbomsByQueryPaginated(
                        pageIndex,
                        pageSize,
                        "rootPurl=='pkg:maven/org.apache.logging.log4j/log4j@2.19.0.redhat-00001?type=pom'",
                        "creationTime=desc="))
                .thenReturn(pagedSboms);
        Mockito.when(
                sbomService.searchSbomsByQueryPaginated(
                        pageIndex,
                        pageSize,
                        "statusMessage=='all went well'",
                        "creationTime=desc="))
                .thenReturn(pagedSboms);
        Mockito.when(
                sbomService.searchSbomsByQueryPaginated(
                        pageIndex,
                        pageSize,
                        "statusMessage=='all*'",
                        "creationTime=desc="))
                .thenReturn(pagedSboms);
        Mockito.when(
                sbomService.searchSbomsByQueryPaginated(
                        pageIndex,
                        pageSize,
                        "statusMessage=='*went*'",
                        "creationTime=desc="))
                .thenReturn(pagedSboms);

        given().when()
                .contentType(ContentType.JSON)
                .request(
                        "GET",
                        "/api/v1alpha1/sboms?pageIndex=" + pageIndex + "&pageSize=" + pageSize + "&query=id==12345")
                .then()
                .statusCode(200)
                .body("content.id", CoreMatchers.hasItem("12345"))
                .and()
                .body("content.identifier", CoreMatchers.hasItem("AWI7P3EJ23YAA"));

        given().when()
                .contentType(ContentType.JSON)
                .request(
                        "GET",
                        "/api/v1alpha1/sboms?pageIndex=" + pageIndex + "&pageSize=" + pageSize
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
                        "/api/v1alpha1/sboms?pageIndex=" + pageIndex + "&pageSize=" + pageSize
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
                        "/api/v1alpha1/sboms?pageIndex=" + pageIndex + "&pageSize=" + pageSize
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
                        "/api/v1alpha1/sboms?pageIndex=" + pageIndex + "&pageSize=" + pageSize
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
                        "/api/v1alpha1/sboms?pageIndex=" + pageIndex + "&pageSize=" + pageSize
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
                        "/api/v1alpha1/sboms?pageIndex=" + pageIndex + "&pageSize=" + pageSize
                                + "&query=statusMessage=='*went*'")
                .then()
                .statusCode(200)
                .body("content.id", CoreMatchers.hasItem("12345"))
                .and()
                .body("content.identifier", CoreMatchers.hasItem("AWI7P3EJ23YAA"));
    }

    @Test
    public void testLegacyRSQLSearch() throws Exception {
        int pageIndex = 0;
        int pageSize = 50;

        Page<Sbom> pagedSboms = initializeOneResultPaginated(pageIndex, pageSize);

        Mockito.when(
                sbomService.searchSbomsByQueryPaginated(
                        pageIndex,
                        pageSize,
                        "buildId==AWI7P3EJ23YAA",
                        "creationTime=desc="))
                .thenReturn(pagedSboms);
        Mockito.when(
                sbomService.searchSbomsByQueryPaginated(
                        pageIndex,
                        pageSize,
                        "buildId=eq=AWI7P3EJ23YAA",
                        "creationTime=desc="))
                .thenReturn(pagedSboms);

        given().when()
                .contentType(ContentType.JSON)
                .request(
                        "GET",
                        "/api/v1alpha1/sboms?pageIndex=" + pageIndex + "&pageSize=" + pageSize
                                + "&query=buildId==AWI7P3EJ23YAA")
                .then()
                .statusCode(200)
                .body("content.id", CoreMatchers.hasItem("12345"))
                .and()
                .body("content.identifier", CoreMatchers.hasItem("AWI7P3EJ23YAA"));

        given().when()
                .contentType(ContentType.JSON)
                .request(
                        "GET",
                        "/api/v1alpha1/sboms?pageIndex=" + pageIndex + "&pageSize=" + pageSize
                                + "&query=buildId=eq=AWI7P3EJ23YAA")
                .then()
                .statusCode(200)
                .body("content.id", CoreMatchers.hasItem("12345"))
                .and()
                .body("content.identifier", CoreMatchers.hasItem("AWI7P3EJ23YAA"));
    }

    @Test
    public void testRSQLSearchNotNullValues() throws Exception {
        int pageIndex = 0;
        int pageSize = 50;
        Page<Sbom> pagedSboms = initializeOneResultPaginated(pageIndex, pageSize);

        Mockito.when(
                sbomService.searchSbomsByQueryPaginated(
                        pageIndex,
                        pageSize,
                        "rootPurl=isnull=false",
                        "creationTime=desc="))
                .thenReturn(pagedSboms);

        given().when()
                .contentType(ContentType.JSON)
                .request(
                        "GET",
                        "/api/v1alpha1/sboms?pageIndex=" + pageIndex + "&pageSize=" + pageSize
                                + "&query=rootPurl=isnull=false")
                .then()
                .statusCode(200)
                .body("content.id", CoreMatchers.hasItem("12345"))
                .and()
                .body("content.identifier", CoreMatchers.hasItem("AWI7P3EJ23YAA"));
    }

    @Test
    public void testRSQLSearchNullValues() throws Exception {
        int pageIndex = 0;
        int pageSize = 50;
        Page<Sbom> pagedSboms = initializeOneResultPaginated(pageIndex, pageSize);

        Mockito.when(
                sbomService
                        .searchSbomsByQueryPaginated(pageIndex, pageSize, "rootPurl=isnull=true", "creationTime=desc="))
                .thenReturn(pagedSboms);

        given().when()
                .contentType(ContentType.JSON)
                .request(
                        "GET",
                        "/api/v1alpha1/sboms?pageIndex=" + pageIndex + "&pageSize=" + pageSize
                                + "&query=rootPurl=isnull=true")
                .then()
                .statusCode(200)
                .body("content.id", CoreMatchers.hasItem("12345"))
                .and()
                .body("content.identifier", CoreMatchers.hasItem("AWI7P3EJ23YAA"));

    }

    @Test
    public void testRSQLSearchNotAllowedProperty() {
        String msg = "RSQL on field Sbom.sbom with type JsonNode is not supported!";

        given().when()
                .contentType(ContentType.JSON)
                .request("GET", "/api/v1alpha1/sboms?query=sbom==null")
                .then()
                .statusCode(400)
                .body(CoreMatchers.equalTo(msg));
    }

    @Test
    public void testRSQLSearchAndLogicalNode() throws Exception {

        int pageIndex = 0;
        int pageSize = 50;

        Page<Sbom> oneContentPage = initializeOneResultPaginated(1, 1);
        Page<Sbom> twoContentPage = initializeTwoResultsPaginated(1, 2);

        Mockito.when(
                sbomService.searchSbomsByQueryPaginated(
                        pageIndex,
                        pageSize,
                        "identifier=eq=AWI7P3EJ23YAA",
                        "creationTime=desc="))
                .thenReturn(oneContentPage);
        Mockito.when(
                sbomService.searchSbomsByQueryPaginated(
                        pageIndex,
                        pageSize,
                        "rootPurl=eq='pkg:maven/org.apache.logging.log4j/log4j@2.19.0.redhat-00001?type=pom'",
                        "creationTime=desc="))
                .thenReturn(oneContentPage);
        Mockito.when(
                sbomService.searchSbomsByQueryPaginated(
                        pageIndex,
                        pageSize,
                        "identifier=eq=AWI7P3EJ23YAA;rootPurl=eq='pkg:maven/org.apache.logging.log4j/log4j@2.19.0.redhat-00001?type=pom'",
                        "creationTime=desc="))
                .thenReturn(oneContentPage);
        given().when()
                .contentType(ContentType.JSON)
                .request(
                        "GET",
                        "/api/v1alpha1/sboms?pageIndex=" + pageIndex + "&pageSize=" + pageSize
                                + "&query=identifier=eq=AWI7P3EJ23YAA;rootPurl=eq='pkg:maven/org.apache.logging.log4j/log4j@2.19.0.redhat-00001?type=pom'")
                .then()
                .statusCode(200)
                .body("content.id", CoreMatchers.hasItem("12345"))
                .and()
                .body("content.identifier", CoreMatchers.hasItem("AWI7P3EJ23YAA"));

        Mockito.when(
                sbomService.searchSbomsByQueryPaginated(
                        pageIndex,
                        pageSize,
                        "identifier=eq=AWI7P3EJ23YAA,identifier=eq=AWI7P3EJ23YAB",
                        "id=asc="))
                .thenReturn(twoContentPage);

        given().when()
                .contentType(ContentType.JSON)
                .request(
                        "GET",
                        "/api/v1alpha1/sboms?pageIndex=" + pageIndex + "&pageSize=" + pageSize
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
                        "/api/v1alpha1/sboms?pageIndex=" + pageIndex + "&pageSize=" + pageSize
                                + "&query=rootPurl=eq='pkg:maven/org.apache.logging.log4j/log4j@2.19.0.redhat-00001?type=pom'")
                .then()
                .statusCode(200)
                .body("content.id", CoreMatchers.hasItem("12345"))
                .and()
                .body("content.identifier", CoreMatchers.hasItem("AWI7P3EJ23YAA"));
    }

    @Test
    public void testRSQLSearchAndLogicalNodeWithSorting() throws Exception {

        int pageIndex = 0;
        int pageSize = 20;

        Page<Sbom> twoContentPage = initializeTwoResultsPaginated(1, 2);

        Mockito.when(
                sbomService.searchSbomsByQueryPaginated(
                        pageIndex,
                        pageSize,
                        "identifier=eq=AWI7P3EJ23YAA,identifier=eq=AWI7P3EJ23YAB",
                        "id=asc="))
                .thenReturn(twoContentPage);

        String response = given().when()
                .contentType(ContentType.JSON)
                .request(
                        "GET",
                        "/api/v1alpha1/sboms?pageIndex=" + pageIndex + "&pageSize=" + pageSize
                                + "&query=identifier=eq=AWI7P3EJ23YAA,identifier=eq=AWI7P3EJ23YAB&sort=id=asc=")
                .then()
                .statusCode(200)
                .extract()
                .asString();

        JsonPath jsonPath = new JsonPath(response);
        assertTrue(jsonPath.getString("content.id[0]").compareTo(jsonPath.getString("content.id[1]")) < 1);

        Mockito.when(
                sbomService.searchSbomsByQueryPaginated(
                        pageIndex,
                        pageSize,
                        "identifier=eq=AWI7P3EJ23YAA,identifier=eq=AWI7P3EJ23YAB",
                        "creationTime=desc="))
                .thenReturn(initializeTwoResultsPaginatedInverted(1, 2));

        response = given().when()
                .contentType(ContentType.JSON)
                .request(
                        "GET",
                        "/api/v1alpha1/sboms?pageIndex=" + pageIndex + "&pageSize=" + pageSize
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

    private Page<Sbom> initializeOneResultPaginated(int pageIndex, int pageSize) throws Exception {
        int totalHits = 1;
        int totalPages = (int) Math.ceil((double) totalHits / (double) pageSize);
        return new Page<Sbom>(pageIndex, pageSize, totalPages, totalHits, Arrays.asList(createFirstSbom()));
    }

    private Page<Sbom> initializeTwoResultsPaginated(int pageIndex, int pageSize) throws Exception {
        int totalHits = 2;
        int totalPages = (int) Math.ceil((double) totalHits / (double) pageSize);
        return new Page<Sbom>(
                pageIndex,
                pageSize,
                totalPages,
                totalHits,
                Arrays.asList(createFirstSbom(), createSecondSbom()));
    }

    private Page<Sbom> initializeTwoResultsPaginatedInverted(int pageIndex, int pageSize) throws Exception {
        int totalHits = 2;
        int totalPages = (int) Math.ceil((double) totalHits / (double) pageSize);
        return new Page<Sbom>(
                pageIndex,
                pageSize,
                totalPages,
                totalHits,
                Arrays.asList(createSecondSbom(), createFirstSbom()));
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

        return sbom;
    }

    private Sbom createSecondSbom() {
        Sbom sbom = new Sbom();
        sbom.setId("54321");
        sbom.setIdentifier("AWI7P3EJ23YAB");
        sbom.setRootPurl("pkg:maven/org.apache.logging.log4j/log4j@2.119.0.redhat-00002?type=pom");
        sbom.setStatusMessage("all went well, again");
        sbom.setCreationTime(Instant.now());
        return sbom;

    }

}
