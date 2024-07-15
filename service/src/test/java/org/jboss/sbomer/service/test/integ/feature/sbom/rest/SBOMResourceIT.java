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
package org.jboss.sbomer.service.test.integ.feature.sbom.rest;

import static io.restassured.RestAssured.given;

import java.io.IOException;
import java.util.Optional;

import org.cyclonedx.model.Bom;
import org.eclipse.microprofile.config.Config;
import org.hamcrest.CoreMatchers;
import org.jboss.sbomer.core.features.sbom.rest.Page;
import org.jboss.sbomer.core.features.sbom.utils.SbomUtils;
import org.jboss.sbomer.core.test.TestResources;
import org.jboss.sbomer.service.feature.sbom.config.features.UmbConfig;
import org.jboss.sbomer.service.feature.sbom.config.features.UmbConfig.UmbProducerConfig;
import org.jboss.sbomer.service.feature.sbom.k8s.model.SbomGenerationStatus;
import org.jboss.sbomer.service.feature.sbom.model.Sbom;
import org.jboss.sbomer.service.feature.sbom.model.SbomGenerationRequest;
import org.jboss.sbomer.service.feature.sbom.service.SbomService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.test.junit.QuarkusTest;
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
class SBOMResourceIT {
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
            Mockito.when(producerConfig.topic()).thenReturn(Optional.of("/Vitualtopic/dummy"));

            Mockito.when(umbConfigSpy.isEnabled()).thenReturn(true);
            Mockito.when(umbConfigSpy.producer()).thenReturn(producerConfig);

            return umbConfigSpy;
        }
    }

    @InjectSpy
    SbomService sbomService;

    // @InjectSpy
    // UmbConfig umbConfig;

    @Test
    void testExistenceOfSbomsEndpoint() {
        Mockito.when(sbomService.searchSbomsByQueryPaginated(0, 50, null, null)).thenReturn(new Page<>());
        given().when()
                .get("/api/v1alpha2/sboms")
                .then()
                .statusCode(200)
                .body("totalHits", CoreMatchers.is(2))
                .and()
                .body("content[0].generationRequest.id", CoreMatchers.is("AASSBB"));
    }

    @Test
    void testListSbomsPageParams() {
        Mockito.when(sbomService.searchSbomsByQueryPaginated(1, 20, null, null)).thenReturn(new Page<>());
        given().when().get("/api/v1alpha2/sboms?pageIndex=1&pageSize=20").then().statusCode(200);
    }

    @Test
    void testGetSbomByIdShouldNotFailForMissing() throws IOException {
        given().when()
                .contentType(ContentType.JSON)
                .request("GET", "/api/v1alpha2/sboms/5644785")
                .then()
                .statusCode(404)
                .body("message", CoreMatchers.is("SBOM with id '5644785' not found"))
                .and()
                .body("errorId", CoreMatchers.isA(String.class));
    }

    @Test
    void testGetSbomById() throws IOException {
        Sbom sbom = new Sbom();
        sbom.setIdentifier("AAAABBBB");
        sbom.setId("12345");

        Mockito.when(sbomService.get("12345")).thenReturn(sbom);

        given().when()
                .contentType(ContentType.JSON)
                .request("GET", "/api/v1alpha2/sboms/12345")
                .then()
                .statusCode(200)
                .body("id", CoreMatchers.equalTo("12345"))
                .and()
                .body("buildId", CoreMatchers.equalTo("AAAABBBB"));
    }

    @Test
    void testGetSbomById_V3() throws IOException {
        Sbom sbom = new Sbom();
        sbom.setIdentifier("AAAABBBB");
        sbom.setId("12345");

        Mockito.when(sbomService.get("12345")).thenReturn(sbom);

        given().when()
                .contentType(ContentType.JSON)
                .request("GET", "/api/v1alpha3/sboms/12345")
                .then()
                .statusCode(200)
                .body("id", CoreMatchers.equalTo("12345"))
                .and()
                .body("identifier", CoreMatchers.equalTo("AAAABBBB"));
    }

    @Test
    void testGetBomById() throws IOException {
        Sbom sbom = new Sbom();
        sbom.setIdentifier("AAAABBBB");
        sbom.setId("12345");

        String bomJson = TestResources.asString("sboms/complete_sbom.json");
        sbom.setSbom(new ObjectMapper().readTree(bomJson));

        Mockito.when(sbomService.get("12345")).thenReturn(sbom);

        given().when()
                .contentType(ContentType.JSON)
                .request("GET", "/api/v1alpha2/sboms/12345/bom")
                .then()
                .statusCode(200)
                .body(
                        "metadata.component.purl",
                        CoreMatchers.equalTo(
                                "pkg:maven/org.eclipse.microprofile.graphql/microprofile-graphql-parent@1.1.0.redhat-00008?type=pom"))
                .and()
                .body("bomFormat", CoreMatchers.equalTo("CycloneDX"));
    }

    @Test
    void testGetSbomByIdShouldHandleIncorrecInput() throws IOException {
        given().when()
                .contentType(ContentType.JSON)
                .request("GET", "/api/v1alpha2/sboms/fgETHHG4785")
                .then()
                .statusCode(404)
                .body("message", CoreMatchers.is("SBOM with id 'fgETHHG4785' not found"))
                .and()
                .body("errorId", CoreMatchers.isA(String.class));
    }

    @Test
    void ensureValidLicense() throws IOException {

        Sbom sbom = new Sbom();
        sbom.setIdentifier("AAAABBBB");
        sbom.setId("12345");

        String bomJson = TestResources.asString("sboms/complete_sbom.json");
        sbom.setSbom(new ObjectMapper().readTree(bomJson));

        Mockito.when(sbomService.get("12345")).thenReturn(sbom);

        given().when()
                .contentType(ContentType.JSON)
                .request("GET", "/api/v1alpha2/sboms/12345/bom")
                .then()
                .statusCode(200)
                .body("metadata.component.name", CoreMatchers.equalTo("microprofile-graphql-parent"))
                .and()
                .body("metadata.component.version", CoreMatchers.equalTo("1.1.0.redhat-00008"))
                .and()
                .body("metadata.component.licenses[0].license.id", CoreMatchers.equalTo("Apache-2.0"));
    }

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
                .body("status", CoreMatchers.is("NEW"));
    }

    @Test
    void shouldStartGenerationForAGivenPncBuildWithEmptyJsonConfig() {
        given().body("{}")
                .when()
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
                .body("status", CoreMatchers.is("NEW"));
    }

    /**
     * Invalid properties are ignored.
     */
    @Test
    void shouldStartGenerationForAGivenPncBuildWithInvalidJsonConfig() {
        given().body("{\"df\": \"123\"}")
                .when()
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
                .body("status", CoreMatchers.is("NEW"));
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
                .statusCode(500)
                .body(
                        "message",
                        CoreMatchers.is(
                                "An error occurred while deserializing provided content: Could not resolve type id 'operation' as a subtype of `org.jboss.sbomer.core.features.sbom.config.PncBuildConfig`: Class `org.jboss.sbomer.core.features.sbom.config.OperationConfig` not subtype of `org.jboss.sbomer.core.features.sbom.config.PncBuildConfig`"))
                .and()
                .body("errorId", CoreMatchers.isA(String.class))
                .and()
                .body("error", CoreMatchers.equalTo("Internal Server Error"));
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
    @ValueSource(strings = { "v1alpha1", "v1alpha2", "v1alpha3" })
    void testUmbNotificationResend(String apiVersion) throws IOException {
        Bom bom = SbomUtils.fromString(TestResources.asString("sboms/sbom_with_errata.json"));

        SbomGenerationRequest generationRequest = SbomGenerationRequest.builder()
                .withId("AABB")
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

        RestAssured.given().when().post("/api/" + apiVersion + "/sboms/BIDBID/notify").then().statusCode(200);
    }
}
