package org.redhat.sbomer.test;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.with;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
public class SBOMResourceTest {

    @Test
    public void testExistenceOfSbomsEndpoint() {
        given()
                .when().get("/sboms")
                .then()
                .statusCode(200);
    }

    @Test
    public void testShouldAcceptValidSBOM() throws IOException {
        with().body(TestResources.asString("sboms/bom-3.9.0-cx-2.7.5.json"))
                .when()
                .contentType(ContentType.JSON)
                .request("POST", "/sboms")
                .then()
                .statusCode(201);
    }

    @Test
    public void testShouldNotAcceptInvalidJson() throws IOException {
        with().body(TestResources.asString("sboms/invalid-json.json"))
                .when()
                .contentType(ContentType.JSON)
                .request("POST", "/sboms")
                .then()
                .statusCode(400);
    }

}