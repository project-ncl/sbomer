package org.jboss.sbomer.func.pnc.test;

import static org.hamcrest.Matchers.notNullValue;

import java.util.UUID;

import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
public class RestTest {

    @Test
    void testSuccess() {
        RestAssured.given()
                .contentType("application/json")
                .header("ce-specversion", "1.0")
                .header("ce-id", UUID.randomUUID().toString())
                .header("ce-type", "org.jboss.sbomer.generation.pnc.v1.request")
                .header("ce-source", "test")
                .body("{\"spec\": {\"buildId\": \"1234\"}}")
                .post("/")
                .then()
                .log()
                .all(true)
                .statusCode(200)
                .header("ce-id", notNullValue())
                .header("ce-type", "org.jboss.sbomer.generation.pnc.v1.success")
                .header("ce-source", "/az/us-east-1/ns/namespace-name/pod/abc")
                .body("spec.bom", CoreMatchers.is("1234"));
    }
}
