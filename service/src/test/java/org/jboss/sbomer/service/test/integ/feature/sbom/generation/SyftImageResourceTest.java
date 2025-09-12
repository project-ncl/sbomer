package org.jboss.sbomer.service.test.integ.feature.sbom.generation;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.hamcrest.CoreMatchers;
import org.jboss.sbomer.core.config.request.ImageRequestConfig;
import org.jboss.sbomer.core.dto.v1beta1.V1Beta1RequestRecord;
import org.jboss.sbomer.service.feature.sbom.service.SbomService;
import org.jboss.sbomer.service.test.utils.umb.TestUmbProfile;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.junit.mockito.InjectSpy;
import io.restassured.http.ContentType;

@QuarkusTest
@TestProfile(TestUmbProfile.class)
class SyftImageResourceTest {

    @InjectSpy
    SbomService sbomService;

    @Nested
    class V1Alpha3 {
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
        void shouldForbidInvalidConfig() {
            given().body("{\"type\": \"syft-image\", \"wrong\": []}")
                    .when()
                    .contentType(ContentType.JSON)
                    .request("POST", "/api/v1alpha3/generator/syft/image/image-name")
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
        void shouldAllowCustomConfig() {
            given().body("{\"type\": \"syft-image\", \"paths\": [ \"/opt\", \"/etc\"]}")
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

    @Nested
    class V1Beta1 {
        @Test
        void shouldNotAllowEmptyConfig() {
            given().body("{}")
                    .when()
                    .contentType(ContentType.JSON)
                    .request("POST", "/api/v1beta1/generations")
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
        void shouldForbidInvalidConfig() {
            given().body("{\"type\": \"syft-image\", \"wrong\": []}")
                    .when()
                    .contentType(ContentType.JSON)
                    .request("POST", "/api/v1beta1/generations")
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
        void shouldAllowCustomConfig() {
            V1Beta1RequestRecord record = given().body("{\"type\": \"image\", \"image\": \"registry.com/image:tag\"}")
                    .when()
                    .contentType(ContentType.JSON)
                    .request("POST", "/api/v1beta1/generations")
                    .then()
                    .statusCode(202)
                    .extract()
                    .as(V1Beta1RequestRecord.class);

            assertNotNull(record.id());
            assertNotNull(record.receivalTime());
            assertEquals("REST", record.eventType().toString());
            assertEquals("IN_PROGRESS", record.eventStatus().toString());

            ImageRequestConfig config = (ImageRequestConfig) record.requestConfig();
            assertEquals("registry.com/image:tag", config.getImage());
        }
    }
}
