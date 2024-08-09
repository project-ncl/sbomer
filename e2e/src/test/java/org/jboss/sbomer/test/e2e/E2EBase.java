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
package org.jboss.sbomer.test.e2e;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.awaitility.Awaitility;
import org.hamcrest.CoreMatchers;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class E2EBase {
    public abstract String datagrepperUriPropertyName();

    public abstract String sbomerUriPropertyName();

    protected String getSbomerBaseUri() {
        return validateProperty(sbomerUriPropertyName());
    }

    protected String getDatagrepperBaseUri() {
        return validateProperty(datagrepperUriPropertyName());
    }

    private String validateProperty(String propertyName) {
        String uri = System.getProperty(propertyName);

        if (uri == null) {
            throw new RuntimeException(
                    String.format(
                            "Could not obtain required property, make sure you set the '-D%s' property",
                            propertyName));
        }

        return uri;
    }

    protected void waitForGeneration(String generationRequestId) {
        waitForGeneration(generationRequestId, 20, TimeUnit.MINUTES);
    }

    protected void waitForGeneration(String generationRequestId, long time, TimeUnit unit) {
        Awaitility.await().atMost(time, unit).pollInterval(5, TimeUnit.SECONDS).until(() -> {
            final Response response = getGeneration(generationRequestId);
            final String status = response.body().jsonPath().getString("status");

            log.info(
                    "GenerationRequest '{}' (type: '{}', identifier: '{}') current status: {}",
                    generationRequestId,
                    response.body().jsonPath().getString("type"),
                    response.body().jsonPath().getString("identifier"),
                    status);

            if (status.equals("FAILED")) {
                log.error("GenerationRequest '{}' failed: {}", generationRequestId, response.asPrettyString());
                throw new Exception(
                        String.format("GenerationRequest '%s' failed, see logs above", generationRequestId));
            }

            return status.equals("FINISHED");
        });

        log.info("GenerationRequest '{}' successfully finished", generationRequestId);
    }

    public void lastCompleteUmbMessageResponse(String generationRequestId, Consumer<Response> consumer) {
        AtomicReference<Response> atomicResponse = new AtomicReference<>();

        Awaitility.await().atMost(2, TimeUnit.MINUTES).pollInterval(5, TimeUnit.SECONDS).until(() -> {

            log.info("Finding last UMB message available for GenerationRquest: '{}'", generationRequestId);

            Response response = RestAssured.given()
                    .baseUri(getDatagrepperBaseUri())
                    .param("delta", "43200") // 12 hours in seconds
                    .param("topic", "/topic/VirtualTopic.eng.pnc.sbom.complete")
                    .param("contains", "\"generationRequest\":{\"id\":\"" + generationRequestId + "\"}")
                    .param("rows_per_page", 1)
                    .param("order", "desc")
                    .get("/raw");

            if (response.body().jsonPath().getInt("count") == 0) {
                log.debug("No UMB messages found for GenerationRquest '{}'", generationRequestId);
                return false;
            }

            log.info(
                    "Message with ID '{}' found for GenerationRquest: '{}'!",
                    response.body().jsonPath().getString("msg_id"),
                    generationRequestId);

            atomicResponse.set(response);

            return true;
        });

        consumer.accept(atomicResponse.get());
    }

    public Response getGeneration(String generationId) {
        return RestAssured.given()
                .baseUri(getSbomerBaseUri())
                .contentType(ContentType.JSON)
                .when()
                .get(String.format("/api/v1alpha3/sboms/requests/%s", generationId));
    }

    public String requestGeneration(String buildId) {
        log.info("Requesting SBOM for build ID: {}", buildId);

        Response response = RestAssured.given()
                .baseUri(getSbomerBaseUri())
                .when()
                .contentType(ContentType.JSON)
                .post(String.format("/api/v1alpha3/sboms/generate/build/%s", buildId));

        response.then()
                .statusCode(202)
                .body("identifier", CoreMatchers.is(buildId))
                .and()
                .body("status", CoreMatchers.is("NEW"));

        return response.body().path("id").toString();
    }

    public String requestContainerImageGeneration(String image, String jsonBody)
            throws UnsupportedEncodingException, MalformedURLException, URISyntaxException {
        log.info("Requesting SBOM for container image: '{}', with jsonBody: '{}'", image, jsonBody);

        Response response = RestAssured.given()
                .urlEncodingEnabled(false)
                .baseUri(getSbomerBaseUri())
                .log()
                .all()
                .when()
                .contentType(ContentType.JSON)
                .body(jsonBody)
                .urlEncodingEnabled(true)
                .post("/api/v1alpha3/generator/syft/image/{image}", image);

        response.then()
                .log()
                .all()
                .statusCode(202)
                .body("identifier", CoreMatchers.is(image))
                .and()
                .body("status", CoreMatchers.is("NEW"));

        return response.body().path("id").toString();
    }

    public String requestGenerationWithConfiguration(String buildId, String jsonBody) {
        log.info("Requesting SBOM for build ID: {}, with jsonBody: {}", buildId, jsonBody);

        Response response = RestAssured.given()
                .baseUri(getSbomerBaseUri())
                .contentType(ContentType.JSON)
                .body(jsonBody)
                .when()
                .contentType(ContentType.JSON)
                .post(String.format("/api/v1alpha3/sboms/generate/build/%s", buildId));

        // We are providing the configuration so the status will jump to INITIALIZING, not NEW
        response.then()
                .statusCode(202)
                .body("identifier", CoreMatchers.is(buildId))
                .and()
                .body("status", CoreMatchers.is("INITIALIZING"));

        return response.body().path("id").toString();
    }
}
