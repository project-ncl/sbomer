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
package org.redhat.sbomer.test;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.with;

import java.io.IOException;

import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
public class SBOMResourceTest {

    @Test
    public void testExistenceOfSbomsEndpoint() {
        given().when().get("/api/v1/sboms").then().statusCode(200);
    }

    @Test
    public void testShouldAcceptValidSbom() throws IOException {
        with().body(TestResources.asString("payloads/payload-valid.json"))
                .when()
                .contentType(ContentType.JSON)
                .request("POST", "/api/v1/sboms")
                .then()
                .statusCode(201);
    }

    @Test
    public void testInvalidJson() throws IOException {
        with().body(TestResources.asString("payloads/payload-invalid-json.json"))
                .when()
                .contentType(ContentType.JSON)
                .request("POST", "/api/v1/sboms")
                .then()
                .statusCode(400);
    }

    @Test
    public void testShouldNotAcceptMissingSbom() throws IOException {
        with().body(TestResources.asString("payloads/payload-invalid-bom.json"))
                .when()
                .contentType(ContentType.JSON)
                .request("POST", "/api/v1/sboms")
                .then()
                .statusCode(400)
                .body(
                        "messages[0]",
                        CoreMatchers.is(
                                "sbom: not a valid CycloneDX object: bom.specVersion: is missing but it is required, bom.specVdersion: is not defined in the schema and the schema does not allow additional properties"));
    }

}