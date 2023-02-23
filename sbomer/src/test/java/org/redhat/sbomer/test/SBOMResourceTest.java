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
        given().when().get("/sboms").then().statusCode(200);
    }

    @Test
    public void testShouldAcceptValidSbom() throws IOException {
        with().body(TestResources.asString("payloads/payload-valid.json"))
                .when()
                .contentType(ContentType.JSON)
                .request("POST", "/sboms")
                .then()
                .statusCode(201);
    }

    @Test
    public void testInvalidJson() throws IOException {
        with().body(TestResources.asString("payloads/payload-invalid-json.json"))
                .when()
                .contentType(ContentType.JSON)
                .request("POST", "/sboms")
                .then()
                .statusCode(400);
    }

    @Test
    public void testShouldNotAcceptMissingSbom() throws IOException {
        with().body(TestResources.asString("payloads/payload-invalid-bom.json"))
                .when()
                .contentType(ContentType.JSON)
                .request("POST", "/sboms")
                .then()
                .statusCode(400)
                .body(
                        "messages[0]",
                        CoreMatchers.is(
                                "sbom: not a valid CycloneDX object: bom.specVersion: is missing but it is required, bom.specVdersion: is not defined in the schema and the schema does not allow additional properties"));
    }

}