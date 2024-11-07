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
package org.jboss.sbomer.service.test.integ.rest;

import static io.restassured.RestAssured.given;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;

import org.cyclonedx.model.Bom;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.hamcrest.CoreMatchers;
import org.jboss.pnc.dto.DeliverableAnalyzerOperation;
import org.jboss.pnc.dto.requests.DeliverablesAnalysisRequest;
import org.jboss.sbomer.core.config.request.ErrataAdvisoryRequestConfig;
import org.jboss.sbomer.core.config.request.RequestConfig;
import org.jboss.sbomer.core.features.sbom.enums.GenerationRequestType;
import org.jboss.sbomer.core.features.sbom.rest.Page;
import org.jboss.sbomer.core.features.sbom.utils.SbomUtils;
import org.jboss.sbomer.core.test.TestResources;
import org.jboss.sbomer.service.feature.FeatureFlags;
import org.jboss.sbomer.service.feature.sbom.config.features.UmbConfig;
import org.jboss.sbomer.service.feature.sbom.config.features.UmbConfig.UmbProducerConfig;
import org.jboss.sbomer.service.feature.sbom.k8s.model.SbomGenerationStatus;
import org.jboss.sbomer.service.feature.sbom.model.RequestEvent;
import org.jboss.sbomer.service.feature.sbom.model.Sbom;
import org.jboss.sbomer.service.feature.sbom.model.SbomGenerationRequest;
import org.jboss.sbomer.service.feature.sbom.service.AdvisoryService;
import org.jboss.sbomer.service.feature.sbom.service.SbomService;
import org.jboss.sbomer.service.pnc.PncClient;
import org.jboss.sbomer.service.test.utils.umb.TestUmbProfile;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.junit.mockito.InjectSpy;
import io.quarkus.test.kubernetes.client.WithKubernetesTestServer;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.smallrye.config.SmallRyeConfig;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

@QuarkusTest
@WithKubernetesTestServer
@TestProfile(TestUmbProfile.class)
class RestResourceIT {

    public static class UmbConfigProducer {
        @Inject
        Config config;

        @Produces
        @ApplicationScoped
        @io.quarkus.test.Mock
        UmbConfig umbConfig() {
            UmbConfig umbConfig = config.unwrap(SmallRyeConfig.class).getConfigMapping(UmbConfig.class);
            UmbConfig umbConfigSpy = Mockito.spy(umbConfig);

            UmbProducerConfig producerConfig = Mockito.mock(UmbProducerConfig.class);
            Mockito.when(producerConfig.isEnabled()).thenReturn(true);

            Mockito.when(umbConfigSpy.isEnabled()).thenReturn(true);
            Mockito.when(umbConfigSpy.producer()).thenReturn(producerConfig);

            return umbConfigSpy;
        }
    }

    @InjectMock
    @RestClient
    PncClient pncClient;

    @InjectMock
    AdvisoryService advisoryService;

    @InjectSpy
    SbomService sbomService;

    @InjectMock
    FeatureFlags featureFlags;

    @ParameterizedTest
    @EnumSource(TestableApiVersion.class)
    void testListSbomsPageParams(TestableApiVersion apiVersion) {
        Mockito.when(sbomService.searchSbomRecordsByQueryPaginated(1, 20, null, null)).thenReturn(new Page<>());
        given().when()
                .get(String.format("?pageIndex=1&pageSize=20", apiVersion.manifestsPath()))
                .then()
                .statusCode(200);
    }

    @ParameterizedTest
    @EnumSource(TestableApiVersion.class)
    void testGetSbomByIdShouldNotFailForMissing(TestableApiVersion apiVersion) throws IOException {
        given().when()
                .contentType(ContentType.JSON)
                .request("GET", String.format("%s/5644785", apiVersion.manifestsPath()))
                .then()
                .statusCode(404)
                .body("message", CoreMatchers.is("Manifest with provided identifier: '5644785' couldn't be found"))
                .and()
                .body("errorId", CoreMatchers.isA(String.class));
    }

    @ParameterizedTest
    @EnumSource(TestableApiVersion.class)
    void testGetSbomById(TestableApiVersion apiVersion) throws IOException {
        Sbom sbom = new Sbom();
        sbom.setIdentifier("AAAABBBB");
        sbom.setId("12345");

        Mockito.when(sbomService.get("12345")).thenReturn(sbom);

        given().when()
                .contentType(ContentType.JSON)
                .request("GET", String.format("%s/12345", apiVersion.manifestsPath()))
                .then()
                .statusCode(200)
                .body("id", CoreMatchers.equalTo("12345"))
                .and()
                .body("identifier", CoreMatchers.equalTo("AAAABBBB"));
    }

    @ParameterizedTest
    @EnumSource(TestableApiVersion.class)
    void testGetBomById(TestableApiVersion apiVersion) throws IOException {
        Sbom sbom = new Sbom();
        sbom.setIdentifier("AAAABBBB");
        sbom.setId("12345");

        String bomJson = TestResources.asString("sboms/complete_sbom.json");
        sbom.setSbom(new ObjectMapper().readTree(bomJson));

        Mockito.when(sbomService.get("12345")).thenReturn(sbom);

        given().when()
                .contentType(ContentType.JSON)
                .request("GET", String.format("%s/12345/bom", apiVersion.manifestsPath()))
                .then()
                .statusCode(200)
                .body(
                        "metadata.component.purl",
                        CoreMatchers.equalTo(
                                "pkg:maven/org.eclipse.microprofile.graphql/microprofile-graphql-parent@1.1.0.redhat-00008?type=pom"))
                .and()
                .body("bomFormat", CoreMatchers.equalTo("CycloneDX"));
    }

    @ParameterizedTest
    @EnumSource(TestableApiVersion.class)
    void ensureValidLicense(TestableApiVersion apiVersion) throws IOException {

        Sbom sbom = new Sbom();
        sbom.setIdentifier("AAAABBBB");
        sbom.setId("12345");

        String bomJson = TestResources.asString("sboms/complete_sbom.json");
        sbom.setSbom(new ObjectMapper().readTree(bomJson));

        Mockito.when(sbomService.get("12345")).thenReturn(sbom);

        given().when()
                .contentType(ContentType.JSON)
                .request("GET", String.format("%s/12345/bom", apiVersion.manifestsPath()))
                .then()
                .statusCode(200)
                .body("metadata.component.name", CoreMatchers.equalTo("microprofile-graphql-parent"))
                .and()
                .body("metadata.component.version", CoreMatchers.equalTo("1.1.0.redhat-00008"))
                .and()
                .body("metadata.component.licenses[0].license.id", CoreMatchers.equalTo("Apache-2.0"));
    }

    @Nested
    class V1Alpha3 {
        /**
         * It should return a valid response for a generation request for PNC build without any config provided.
         */
        @Test
        void shouldStartGenerationForAGivenPncBuild() {
            given().when()
                    .contentType(ContentType.JSON)
                    .request("POST", "/api/v1alpha3/sboms/generate/build/AABBCC")
                    .then()
                    .statusCode(202)
                    .body("id", CoreMatchers.any(String.class))
                    .and()
                    .body("identifier", CoreMatchers.equalTo("AABBCC"))
                    .and()
                    .body("type", CoreMatchers.equalTo("BUILD"))
                    .and()
                    .body("status", CoreMatchers.is("INITIALIZING"));
        }

        @Test
        void testExistenceOfSbomsEndpoint() {
            Mockito.when(sbomService.searchSbomRecordsByQueryPaginated(0, 50, null, null)).thenReturn(new Page<>());
            given().when()
                    .get("/api/v1alpha3/sboms")
                    .then()
                    .statusCode(200)
                    .body("totalHits", CoreMatchers.is(2))
                    .and()
                    .body("content[0].generationRequest.id", CoreMatchers.is("AASSBB"));
        }

        @Test
        void shouldNotStartGenerationForAGivenPncBuildWithEmptyJsonConfig() {
            given().body("{}")
                    .when()
                    .contentType(ContentType.JSON)
                    .request("POST", "/api/v1alpha3/sboms/generate/build/AABBCC")
                    .then()
                    .statusCode(400)
                    .body(
                            "message",
                            CoreMatchers.is(
                                    "An error occurred while deserializing provided content, please check your body 🤼"))
                    .and()
                    .body("errorId", CoreMatchers.isA(String.class))
                    .and()
                    .body("error", CoreMatchers.equalTo("Bad Request"));
        }

        @Test
        void shouldNotStartGenerationForAGivenPncBuildWithInvalidJsonConfig() {
            given().body("{\"df\": \"123\"}")
                    .when()
                    .contentType(ContentType.JSON)
                    .request("POST", "/api/v1alpha3/sboms/generate/build/AABBCC")
                    .then()
                    .statusCode(400)
                    .body(
                            "message",
                            CoreMatchers.is(
                                    "An error occurred while deserializing provided content, please check your body 🤼"))
                    .and()
                    .body("errorId", CoreMatchers.isA(String.class))
                    .and()
                    .body("error", CoreMatchers.equalTo("Bad Request"));
        }

        /**
         * Invalid configs cause failures.
         */
        @Test
        void shouldStartGenerationForAGivenPncBuildWithInvalidConfigType() {
            given().body("{\"type\": \"operation\"}")
                    .when()
                    .log()
                    .all()
                    .contentType(ContentType.JSON)
                    .request("POST", "/api/v1alpha3/sboms/generate/build/AABBCC")
                    .then()
                    .statusCode(400)
                    .body(
                            "message",
                            CoreMatchers.is(
                                    "An error occurred while deserializing provided content, please check your body 🤼"))
                    .and()
                    .body("errorId", CoreMatchers.isA(String.class))
                    .and()
                    .body("error", CoreMatchers.equalTo("Bad Request"));
        }
    }

    @Nested
    class V1Beta1 {
        private static final String API_VERSION = "v1beta1";

        private final String requestApiPath = String.format("/api/%s/generations", API_VERSION);

        @Test
        void shouldHandleInvalidConfig() {
            given().body("{\"df\": \"123\"}")
                    .when()
                    .contentType(ContentType.JSON)
                    .request("POST", requestApiPath)
                    .then()
                    .log()
                    .all(true)
                    .statusCode(400)
                    .body(
                            "message",
                            CoreMatchers.is(
                                    "An error occurred while deserializing provided content, please check your body 🤼"))
                    .and()
                    .body("errorId", CoreMatchers.isA(String.class))
                    .and()
                    .body("error", CoreMatchers.equalTo("Bad Request"));
        }

        @Test
        void testExistenceOfSbomsEndpoint() {
            Mockito.when(sbomService.searchSbomRecordsByQueryPaginated(0, 50, null, null)).thenReturn(new Page<>());
            given().when()
                    .get("/api/v1beta1/manifests")
                    .then()
                    .statusCode(200)
                    .body("totalHits", CoreMatchers.is(2))
                    .and()
                    .body("content[0].generation.id", CoreMatchers.is("AASSBB"));
        }

        @Test
        void shouldRequestPncBuild() {
            given().body("{\"type\": \"pnc-build\", \"buildId\": \"AABBCC\"}")
                    .when()
                    .contentType(ContentType.JSON)
                    .request("POST", requestApiPath)
                    .then()
                    .log()
                    .all(true)
                    .statusCode(202)
                    .body("size()", CoreMatchers.is(1))
                    .and()
                    .body("[0].id", CoreMatchers.any(String.class))
                    .and()
                    .body("[0].identifier", CoreMatchers.equalTo("AABBCC"))
                    .and()
                    .body("[0].type", CoreMatchers.equalTo("BUILD"))
                    .and()
                    .body("[0].status", CoreMatchers.is("INITIALIZING"));
        }

        @Test
        void shouldRequestContainerImage() {
            given().body("{\"type\": \"image\", \"image\": \"registry.com/image:tag\"}")
                    .when()
                    .contentType(ContentType.JSON)
                    .request("POST", requestApiPath)
                    .then()
                    .log()
                    .all(true)
                    .statusCode(202)
                    .body("size()", CoreMatchers.is(1))
                    .and()
                    .body("[0].id", CoreMatchers.any(String.class))
                    .and()
                    .body("[0].identifier", CoreMatchers.equalTo("registry.com/image:tag"))
                    .and()
                    .body("[0].type", CoreMatchers.equalTo("CONTAINERIMAGE"))
                    .and()
                    .body("[0].status", CoreMatchers.is("NEW"));
        }

        @Test
        void shouldHandleMalformedContent() {
            given().body(
                    "{\"type\": \"pnc-analysis\", \"milestoneId\": \"ABCDEF\", \"urls\": \\\"http://host.com/a.zip\\\", \"http://host.com/b.zip\"]}")
                    .when()
                    .contentType(ContentType.JSON)
                    .request("POST", requestApiPath)
                    .then()
                    .log()
                    .all(true)
                    .statusCode(400)
                    .body("message", CoreMatchers.is("Unable to process request: HTTP 400 Bad Request"))
                    .and()
                    .body("errorId", CoreMatchers.isA(String.class))
                    .and()
                    .body("error", CoreMatchers.equalTo("Bad Request"));
        }

        @Test
        void shouldRequestAnalysis() {
            Mockito.when(
                    pncClient.analyzeDeliverables(
                            "ABCDEF",
                            DeliverablesAnalysisRequest.builder()
                                    .deliverablesUrls(List.of("http://host.com/a.zip", "http://host.com/b.zip"))
                                    .build()))
                    .thenReturn(DeliverableAnalyzerOperation.delAnalyzerBuilder().id("RETID").build());

            given().body(
                    "{\"type\": \"pnc-analysis\", \"milestoneId\": \"ABCDEF\", \"urls\": [\"http://host.com/a.zip\", \"http://host.com/b.zip\"]}")
                    .when()
                    .contentType(ContentType.JSON)
                    .request("POST", requestApiPath)
                    .then()
                    .log()
                    .all(true)
                    .statusCode(202)
                    .body("size()", CoreMatchers.is(1))
                    .and()
                    .body("[0].id", CoreMatchers.any(String.class))
                    .and()
                    .body("[0].identifier", CoreMatchers.equalTo("RETID"))
                    .and()
                    .body("[0].type", CoreMatchers.equalTo("OPERATION"))
                    .and()
                    .body("[0].status", CoreMatchers.is("NO_OP"));
        }

        @Test
        void shouldRequestOperation() {
            given().body("{\"type\": \"pnc-operation\", \"operationId\": \"ABCDEF\"}")
                    .when()
                    .contentType(ContentType.JSON)
                    .request("POST", requestApiPath)
                    .then()
                    .log()
                    .all(true)
                    .statusCode(202)
                    .body("size()", CoreMatchers.is(1))
                    .and()
                    .body("[0].id", CoreMatchers.any(String.class))
                    .and()
                    .body("[0].identifier", CoreMatchers.equalTo("ABCDEF"))
                    .and()
                    .body("[0].type", CoreMatchers.equalTo("OPERATION"))
                    .and()
                    .body("[0].status", CoreMatchers.is("NEW"));
        }

        @Test
        void shouldRequestAdvisory() {

            ArgumentMatcher<RequestEvent> hasAdvisoryId = cfg -> cfg != null
                    && "12345".equals(((ErrataAdvisoryRequestConfig) cfg.getRequestConfig()).getAdvisoryId());

            when(advisoryService.generateFromAdvisory(argThat(hasAdvisoryId))).thenReturn(
                    List.of(
                            SbomGenerationRequest.builder()
                                    .withId("SOMEID")
                                    .withIdentifier("AAABBB")
                                    .withType(GenerationRequestType.BREW_RPM)
                                    .withStatus(SbomGenerationStatus.NEW)
                                    .build()));

            given().body("{\"type\": \"errata-advisory\", \"advisoryId\": \"12345\"}")
                    .when()
                    .contentType(ContentType.JSON)
                    .request("POST", requestApiPath)
                    .then()
                    .log()
                    .all(true)
                    .statusCode(202)
                    .body("size()", CoreMatchers.is(1))
                    .and()
                    .body("[0].id", CoreMatchers.any(String.class))
                    .and()
                    .body("[0].identifier", CoreMatchers.equalTo("AAABBB"))
                    .and()
                    .body("[0].type", CoreMatchers.equalTo("BREW_RPM"))
                    .and()
                    .body("[0].status", CoreMatchers.is("NEW"));
        }
    }

    /**
     * Tests UMB notification resend.
     *
     * See {@link UmbConfigProducer} above.
     *
     * @param apiVersion
     * @throws IOException
     */
    @ParameterizedTest
    @EnumSource(TestableApiVersion.class)
    void testUmbNotificationResend(TestableApiVersion apiVersion) throws IOException {
        Mockito.when(featureFlags.shouldNotify(eq(GenerationRequestType.BUILD))).thenReturn(true);

        Bom bom = SbomUtils.fromString(TestResources.asString("sboms/sbom_with_errata.json"));

        SbomGenerationRequest generationRequest = SbomGenerationRequest.builder()
                .withId("AABB")
                .withType(GenerationRequestType.BUILD)
                .withIdentifier("BIDBID")
                .withStatus(SbomGenerationStatus.FINISHED)
                .build();

        Sbom sbom = new Sbom();
        sbom.setIdentifier("BIDBID");
        sbom.setRootPurl(bom.getMetadata().getComponent().getPurl());
        sbom.setId("416640206274228333");
        sbom.setSbom(SbomUtils.toJsonNode(bom));
        sbom.setGenerationRequest(generationRequest);

        Mockito.when(sbomService.get("BIDBID")).thenReturn(sbom);

        RestAssured.given()
                .when()
                .post(String.format("%s/BIDBID/notify", apiVersion.manifestsPath()))
                .then()
                .statusCode(200);
    }
}
