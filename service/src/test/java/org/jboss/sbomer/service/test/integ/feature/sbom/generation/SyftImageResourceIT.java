package org.jboss.sbomer.service.test.integ.feature.sbom.generation;

import static io.restassured.RestAssured.given;

import org.hamcrest.CoreMatchers;
import org.jboss.sbomer.service.feature.sbom.service.SbomService;
import org.jboss.sbomer.service.test.utils.umb.TestUmbProfile;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.junit.mockito.InjectSpy;
import io.quarkus.test.kubernetes.client.WithKubernetesTestServer;
import io.restassured.http.ContentType;

@QuarkusTest
@WithKubernetesTestServer
@TestProfile(TestUmbProfile.class)
class SyftImageResourceIT {

    @InjectSpy
    SbomService sbomService;

    @Test
    void shouldAllowNoConfig() {
        given().when()
                .contentType(ContentType.JSON)
                .request("POST", "/api/v1alpha3/generator/syft/image/image-name")
                .then()
                .statusCode(202)
                .body("id", CoreMatchers.any(String.class))
                .and()
                .body("identifier", CoreMatchers.equalTo("image-name"))
                .and()
                .body("type", CoreMatchers.equalTo("CONTAINERIMAGE"))
                .and()
                .body("status", CoreMatchers.is("NEW"));
    }

    @Test
    void shouldNotAllowEmptyConfig() {
        given().body("{}")
                .when()
                .contentType(ContentType.JSON)
                .request("POST", "/api/v1alpha3/generator/syft/image/image-name")
                .then()
                .statusCode(500)
                .body(
                        "message",
                        CoreMatchers.is(
                                "An error occurred while deserializing provided content: Could not resolve subtype of [simple type, class org.jboss.sbomer.core.features.sbom.config.SyftImageConfig]: missing type id property 'type'"))
                .and()
                .body("errorId", CoreMatchers.isA(String.class))
                .and()
                .body("error", CoreMatchers.equalTo("Internal Server Error"));
    }

    @Test
    @Disabled("This is currently disabled, because we do deserialziation first and only after this we validate it. Deserialization and validation should be done at the same time.")
    void shouldForbidInvalidConfig() {
        given().body("{\"type\": \"syft-image\", \"wrong\": []}")
                .when()
                .contentType(ContentType.JSON)
                .request("POST", "/api/v1alpha3/generator/syft/image/image-name")
                .then()
                .statusCode(500)
                .body(
                        "message",
                        CoreMatchers.is(
                                "An error occurred while deserializing provided content: Could not resolve subtype of [simple type, class org.jboss.sbomer.core.features.sbom.config.SyftImageConfig]: missing type id property 'type'"))
                .and()
                .body("errorId", CoreMatchers.isA(String.class))
                .and()
                .body("error", CoreMatchers.equalTo("Internal Server Error"));
    }

    @Test
    void shouldAllowCustomConfig() {
        given().body("{\"type\": \"syft-image\", \"directories\": [ \"/opt\", \"/etc\"]}")
                .when()
                .contentType(ContentType.JSON)
                .request("POST", "/api/v1alpha3/generator/syft/image/image-name")
                .then()
                .statusCode(202)
                .body("id", CoreMatchers.any(String.class))
                .and()
                .body("identifier", CoreMatchers.equalTo("image-name"))
                .and()
                .body("type", CoreMatchers.equalTo("CONTAINERIMAGE"))
                .and()
                .body("status", CoreMatchers.is("NEW"));
    }
}
