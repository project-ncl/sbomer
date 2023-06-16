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
package org.jboss.sbomer.feature.sbom.core.test;

import static io.restassured.RestAssured.given;

import java.io.IOException;

import org.hamcrest.CoreMatchers;
import org.jboss.sbomer.core.service.rest.Page;
import org.jboss.sbomer.core.test.TestResources;
import org.jboss.sbomer.feature.sbom.core.model.Sbom;
import org.jboss.sbomer.feature.sbom.core.service.SbomService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectSpy;
import io.quarkus.test.kubernetes.client.WithKubernetesTestServer;
import io.restassured.http.ContentType;

@QuarkusTest
@WithKubernetesTestServer
public class SBOMResourceTest {

    @InjectSpy
    SbomService sbomService;

    @Test
    public void testExistenceOfSbomsEndpoint() {
        Mockito.when(sbomService.searchByQueryPaginated(0, 50, null)).thenReturn(new Page<>());
        given().when().get("/api/v1alpha1/sboms").then().statusCode(200);
    }

    @Test
    public void testListSbomsPageParams() {
        Mockito.when(sbomService.searchByQueryPaginated(1, 20, null)).thenReturn(new Page<>());
        given().when().get("/api/v1alpha1/sboms?pageIndex=1&pageSize=20").then().statusCode(200);
    }

    @Test
    public void testGetSbomByIdShouldNotFailForMissing() throws IOException {
        given().when()
                .contentType(ContentType.JSON)
                .request("GET", "/api/v1alpha1/sboms/5644785")
                .then()
                .statusCode(404)
                .body("message", CoreMatchers.is("SBOM with id '5644785' not found"))
                .and()
                .body("errorId", CoreMatchers.isA(String.class));
    }

    @Test
    public void testGetSbomById() throws IOException {
        Sbom sbom = new Sbom();
        sbom.setBuildId("AAAABBBB");
        sbom.setId(12345L);

        Mockito.when(sbomService.get("12345")).thenReturn(sbom);

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
    public void testGetBomById() throws IOException {
        Sbom sbom = new Sbom();
        sbom.setBuildId("AAAABBBB");
        sbom.setId(12345L);

        String bomJson = TestResources.asString("sboms/base.json");
        sbom.setSbom(new ObjectMapper().readTree(bomJson));

        Mockito.when(sbomService.get("12345")).thenReturn(sbom);

        given().when()
                .contentType(ContentType.JSON)
                .request("GET", "/api/v1alpha1/sboms/12345/bom")
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
    public void ensureValidLicense() throws IOException {

        Sbom sbom = new Sbom();
        sbom.setBuildId("AAAABBBB");
        sbom.setId(12345L);

        String bomJson = TestResources.asString("sboms/base.json");
        sbom.setSbom(new ObjectMapper().readTree(bomJson));

        Mockito.when(sbomService.get("12345")).thenReturn(sbom);

        given().when()
                .contentType(ContentType.JSON)
                .request("GET", "/api/v1alpha1/sboms/12345/bom")
                .then()
                .statusCode(200)
                .body("metadata.component.name", CoreMatchers.equalTo("microprofile-graphql-parent"))
                .and()
                .body("metadata.component.version", CoreMatchers.equalTo("1.1.0.redhat-00008"))
                .and()
                .body("metadata.component.licenses[0].license.id", CoreMatchers.equalTo("Apache-2.0"));
    }

    // /**
    // * It should return a stub for the {@link Sbom} object, where the sbom field is empty.
    // *
    // * @throws IOException
    // */
    // @Test
    // public void shouldStartGenerationForAGivenPncBuild() throws IOException {
    // Sbom sbom = new Sbom();
    // sbom.setBuildId("AABBCC");
    // sbom.setId(416640206274228224L);
    // sbom.setType(SbomType.BUILD_TIME);
    // sbom.setGenerationTime(Instant.now());
    // sbom.setGenerator(GeneratorType.MAVEN_CYCLONEDX);

    // Mockito.when(generationService.generate("AABBCC", GeneratorType.MAVEN_CYCLONEDX)).thenReturn(sbom);

    // given().when()
    // .contentType(ContentType.JSON)
    // .request("POST", "/api/v1alpha1/sboms/generate/build/AABBCC")
    // .then()
    // .statusCode(202)
    // .body("id", CoreMatchers.any(Long.class))
    // .and()
    // .body("buildId", CoreMatchers.equalTo("AABBCC"))
    // .and()
    // .body("sbom", CoreMatchers.nullValue())
    // .and()
    // .body("generator", CoreMatchers.is("CYCLONEDX"))
    // .and()
    // .body("processor", CoreMatchers.nullValue());
    // }

}
