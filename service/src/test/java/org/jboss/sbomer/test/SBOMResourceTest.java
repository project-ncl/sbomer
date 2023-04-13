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

import org.hamcrest.CoreMatchers;
import org.jboss.sbomer.core.enums.GeneratorImplementation;
import org.jboss.sbomer.core.enums.ProcessorImplementation;
import org.jboss.sbomer.core.test.TestResources;
import org.jboss.sbomer.model.Sbom;
import org.jboss.sbomer.rest.dto.Page;
import org.jboss.sbomer.service.SbomService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectSpy;
import io.restassured.http.ContentType;

@QuarkusTest
public class SBOMResourceTest {

    @InjectSpy
    SbomService sbomService;

    @Test
    public void testExistenceOfSbomsEndpoint() {
        Mockito.when(sbomService.listSboms(0, 50)).thenReturn(new Page<>());
        given().when().get("/api/v1alpha1/sboms").then().statusCode(200);
    }

    @Test
    public void testListSbomsPageParams() {
        Mockito.when(sbomService.listSboms(1, 20)).thenReturn(new Page<>());
        given().when().get("/api/v1alpha1/sboms?pageIndex=1&pageSize=20").then().statusCode(200);
    }

    @Test
    public void testShouldAcceptValidSbom() throws IOException {
        ArgumentCaptor<Sbom> sbomCapture = ArgumentCaptor.forClass(Sbom.class);

        Mockito.doNothing().when(sbomService).processSbom(sbomCapture.capture(), eq(ProcessorImplementation.DEFAULT));

        with().body(TestResources.asString("payloads/payload-valid.json"))
                .when()
                .contentType(ContentType.JSON)
                .request("POST", "/api/v1alpha1/sboms")
                .then()
                .statusCode(201);

        assertEquals("AWI7P3EJ23YAA", sbomCapture.getValue().getBuildId());
        assertEquals(GeneratorImplementation.CYCLONEDX, sbomCapture.getValue().getGenerator());

        verify(sbomService, times(1)).processSbom(sbomCapture.getValue(), ProcessorImplementation.DEFAULT);
    }

    @Test
    public void testInvalidJson() throws IOException {
        with().body(TestResources.asString("payloads/payload-invalid-json.json"))
                .when()
                .contentType(ContentType.JSON)
                .request("POST", "/api/v1alpha1/sboms")
                .then()
                .statusCode(400);
    }

    @Test
    public void testShouldNotAcceptMissingSbom() throws IOException {
        with().body(TestResources.asString("payloads/payload-invalid-bom.json"))
                .when()
                .contentType(ContentType.JSON)
                .request("POST", "/api/v1alpha1/sboms")
                .then()
                .statusCode(422)
                .body("message", CoreMatchers.equalTo("SBOM validation error"))
                .and()
                .body(
                        "errors",
                        CoreMatchers.hasItems(
                                CoreMatchers.is(
                                        "Invalid CycloneDX object: sbom.specVersion: is missing but it is required"),
                                CoreMatchers.is(
                                        "Invalid CycloneDX object: sbom.specVdersion: is not defined in the schema and the schema does not allow additional properties")));
    }

    @Test
    public void testShouldNotAcceptMissingBuildId() throws IOException {
        with().body(TestResources.asString("payloads/payload-invalid-missing-buildid.json"))
                .when()
                .contentType(ContentType.JSON)
                .request("POST", "/api/v1alpha1/sboms")
                .then()
                .statusCode(422)
                .body("message", CoreMatchers.equalTo("SBOM validation error"))
                .and()
                .body("errors[0]", CoreMatchers.is("buildId: Build identifier missing"));
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

        Mockito.when(sbomService.getSbomById(12345)).thenReturn(sbom);

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
}