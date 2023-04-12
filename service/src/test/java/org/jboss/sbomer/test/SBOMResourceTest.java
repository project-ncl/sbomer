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
package org.jboss.sbomer.test;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.with;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.time.Instant;

import org.hamcrest.CoreMatchers;
import org.jboss.pnc.common.json.JsonUtils;
import org.jboss.sbomer.core.enums.GeneratorImplementation;
import org.jboss.sbomer.core.enums.ProcessorImplementation;
import org.jboss.sbomer.core.enums.SbomType;
import org.jboss.sbomer.core.test.TestResources;
import org.jboss.sbomer.core.utils.UrlUtils;
import org.jboss.sbomer.model.Sbom;
import org.jboss.sbomer.rest.dto.Page;
import org.jboss.sbomer.service.SbomService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import com.fasterxml.jackson.databind.JsonNode;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectSpy;
import io.restassured.http.ContentType;

@QuarkusTest
public class SBOMResourceTest {

    @InjectSpy
    SbomService sbomService;

    @Test
    public void testExistenceOfSbomsEndpoint() {
        Mockito.when(sbomService.list(0, 50)).thenReturn(new Page<>());
        given().when().get("/api/v1alpha1/sboms").then().statusCode(200);
    }

    @Test
    public void testListSbomsPageParams() {
        Mockito.when(sbomService.list(1, 20)).thenReturn(new Page<>());
        given().when().get("/api/v1alpha1/sboms?pageIndex=1&pageSize=20").then().statusCode(200);
    }

    @Test
    public void testGetSbomByIdShouldNotFailForMissing() throws IOException {
        given().when()
                .contentType(ContentType.JSON)
                .request("GET", "/api/v1alpha1/sboms/5644785")
                .then()
                .statusCode(404)
                .body("message", CoreMatchers.is("Sbom with id '5644785' not found"))
                .and()
                .body("errorId", CoreMatchers.isA(String.class));
    }

    @Test
    public void testGetSbomById() throws IOException {
        Sbom sbom = new Sbom();
        sbom.setBuildId("AAAABBBB");
        sbom.setId(12345L);

        Mockito.when(sbomService.get(12345l)).thenReturn(sbom);

        given().when()
                .contentType(ContentType.JSON)
                .request("GET", "/api/v1alpha1/sboms/12345")
                .then()
                .statusCode(200)
                .body("id", CoreMatchers.equalTo(12345))
                .and()
                .body("buildId", CoreMatchers.equalTo("AAAABBBB"));
    }

    @Test
    public void testGetSbomByIdShouldHandleIncorrecInput() throws IOException {
        given().when()
                .contentType(ContentType.JSON)
                .request("GET", "/api/v1alpha1/sboms/fgETHHG4785")
                .then()
                .statusCode(400)
                .body("message", CoreMatchers.is("Invalid SBOM id provided: 'fgETHHG4785', a number was expected"))
                .and()
                .body("errorId", CoreMatchers.isA(String.class));
    }

    @Test
    public void testGetBomOfBaseSbomByBuildId() throws IOException {
        String bom = TestResources.asString("sboms/sbom-valid-parent.json");
        JsonNode sbomJson = JsonUtils.fromJson(bom, JsonNode.class);
        Sbom sbom = new Sbom();
        sbom.setBuildId("ARYT3LBXDVYAC");
        sbom.setId(416640206274228224L);
        sbom.setType(SbomType.BUILD_TIME);
        sbom.setGenerationTime(Instant.now());
        sbom.setSbom(sbomJson);
        sbom.setRootPurl("pkg:maven/cpaas.tp/cpaas-test-pnc-maven@1.0.0.redhat-04562?type=pom");
        sbom.setGenerator(GeneratorImplementation.CYCLONEDX);

        Mockito.when(sbomService.getBaseSbomByBuildId("ARYT3LBXDVYAC")).thenReturn(sbom);

        given().when()
                .contentType(ContentType.JSON)
                .request("GET", "/api/v1alpha1/sboms/build/ARYT3LBXDVYAC/base/bom")
                .then()
                .statusCode(200)
                .body("metadata.component.name", CoreMatchers.equalTo("cpaas-test-pnc-maven"))
                .and()
                .body("metadata.component.version", CoreMatchers.equalTo("1.0.0.redhat-04562"));
    }

    @Test
    public void testGetBomOfBaseSbomByRootPurl() throws IOException {
        String bom = TestResources.asString("sboms/sbom-valid-parent.json");
        JsonNode sbomJson = JsonUtils.fromJson(bom, JsonNode.class);
        String rootPurl = "pkg:maven/cpaas.tp/cpaas-test-pnc-maven@1.0.0.redhat-04562?type=pom";

        Sbom sbom = new Sbom();
        sbom.setBuildId("ARYT3LBXDVYAC");
        sbom.setId(416640206274228224L);
        sbom.setType(SbomType.BUILD_TIME);
        sbom.setGenerationTime(Instant.now());
        sbom.setSbom(sbomJson);
        sbom.setRootPurl(rootPurl);
        sbom.setGenerator(GeneratorImplementation.CYCLONEDX);

        Mockito.when(sbomService.getBaseSbomByRootPurl(rootPurl)).thenReturn(sbom);

        given().when()
                .contentType(ContentType.JSON)
                .request("GET", "/api/v1alpha1/sboms/purl/" + UrlUtils.urlencode(rootPurl) + "/base/bom")
                .then()
                .statusCode(200)
                .body("metadata.component.name", CoreMatchers.equalTo("cpaas-test-pnc-maven"))
                .and()
                .body("metadata.component.version", CoreMatchers.equalTo("1.0.0.redhat-04562"));
    }

    @Test
    public void testGetBomOfEnrichedSbomByBuildId() throws IOException {
        String bom = TestResources.asString("sboms/sbom-valid-enriched-v10.json");
        JsonNode sbomJson = JsonUtils.fromJson(bom, JsonNode.class);
        Sbom sbom = new Sbom();
        sbom.setBuildId("ARYT3LBXDVYAC");
        sbom.setId(416640206274228224L);
        sbom.setType(SbomType.BUILD_TIME);
        sbom.setGenerationTime(Instant.now());
        sbom.setSbom(sbomJson);
        sbom.setRootPurl("pkg:maven/cpaas.tp/cpaas-test-pnc-maven@1.0.0.redhat-04562?type=pom");
        sbom.setGenerator(GeneratorImplementation.CYCLONEDX);
        sbom.setProcessor(ProcessorImplementation.PROPERTIES);

        Mockito.when(sbomService.getEnrichedSbomByBuildId("ARYT3LBXDVYAC")).thenReturn(sbom);

        given().when()
                .contentType(ContentType.JSON)
                .request("GET", "/api/v1alpha1/sboms/build/ARYT3LBXDVYAC/enriched/bom")
                .then()
                .statusCode(200)
                .body("metadata.component.name", CoreMatchers.equalTo("cpaas-test-pnc-maven"))
                .and()
                .body("metadata.component.version", CoreMatchers.equalTo("1.0.0.redhat-04562"))
                .and()
                .body(
                        "components.properties.name",
                        CoreMatchers.hasItem(CoreMatchers.hasItem(CoreMatchers.is("pnc-build-id"))));
    }

    @Test
    public void testGetBomOfEnrichedSbomByRootPurl() throws IOException {
        String bom = TestResources.asString("sboms/sbom-valid-enriched-v10.json");
        JsonNode sbomJson = JsonUtils.fromJson(bom, JsonNode.class);
        String rootPurl = "pkg:maven/cpaas.tp/cpaas-test-pnc-maven@1.0.0.redhat-04562?type=pom";

        Sbom sbom = new Sbom();
        sbom.setBuildId("ARYT3LBXDVYAC");
        sbom.setId(416640206274228224L);
        sbom.setType(SbomType.BUILD_TIME);
        sbom.setGenerationTime(Instant.now());
        sbom.setSbom(sbomJson);
        sbom.setRootPurl(rootPurl);
        sbom.setGenerator(GeneratorImplementation.CYCLONEDX);
        sbom.setProcessor(ProcessorImplementation.PROPERTIES);

        Mockito.when(sbomService.getBaseSbomByRootPurl(rootPurl)).thenReturn(sbom);

        given().when()
                .contentType(ContentType.JSON)
                .request("GET", "/api/v1alpha1/sboms/purl/" + UrlUtils.urlencode(rootPurl) + "/enriched/bom")
                .then()
                .statusCode(200)
                .body("metadata.component.name", CoreMatchers.equalTo("cpaas-test-pnc-maven"))
                .and()
                .body("metadata.component.version", CoreMatchers.equalTo("1.0.0.redhat-04562"))
                .and()
                .body(
                        "components.properties.name",
                        CoreMatchers.hasItem(CoreMatchers.hasItem(CoreMatchers.is("pnc-build-id"))));
    }

    /**
     * It should return a stub for the {@link Sbom} object, where the sbom field is empty.
     *
     * @throws IOException
     */
    @Test
    public void shouldStartGenerationForAGivenPncBuild() throws IOException {
        given().when()
                .contentType(ContentType.JSON)
                .request("POST", "/api/v1alpha1/sboms/generate/build/AABBCC")
                .then()
                .statusCode(202)
                .body("id", CoreMatchers.any(Long.class))
                .and()
                .body("buildId", CoreMatchers.equalTo("AABBCC"))
                .and()
                .body("sbom", CoreMatchers.nullValue())
                .and()
                .body("generator", CoreMatchers.is("CYCLONEDX"))
                .and()
                .body("processor", CoreMatchers.nullValue());
    }
}
