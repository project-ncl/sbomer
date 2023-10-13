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

    public Response givenLastCompleteUmbMessage() {
        return RestAssured.given()
                .baseUri(getDatagrepperBaseUri())
                .param("topic", "/topic/VirtualTopic.eng.pnc.sbom.complete")
                .param("rows_per_page", 1)
                .get("/raw");
    }

    public Response givenLastCompleteUmbMessageForGeneration(String generationRequestId) {
        log.info("Finding last UMB message available for the request: {}", generationRequestId);

        return RestAssured.given()
                .baseUri(getDatagrepperBaseUri())
                .param("delta", "43200") // 12 hours in seconds
                .param("topic", "/topic/VirtualTopic.eng.pnc.sbom.complete")
                .param("contains", generationRequestId)
                .param("rows_per_page", 1)
                .get("/raw");
    }

    public Response getGeneration(String generationId) {
        return RestAssured.given()
                .baseUri(getSbomerBaseUri())
                .contentType(ContentType.JSON)
                .when()
                .get(String.format("/api/v1alpha2/sboms/requests/%s", generationId));
    }

    public String requestGeneration(String buildId) {
        log.info("Requesting SBOM for build ID: {}", buildId);

        Response response = RestAssured.given()
                .baseUri(getSbomerBaseUri())
                .when()
                .contentType(ContentType.JSON)
                .post(String.format("/api/v1alpha2/sboms/generate/build/%s", buildId));

        response.then()
                .statusCode(202)
                .body("buildId", CoreMatchers.is(buildId))
                .and()
                .body("status", CoreMatchers.is("NEW"));

        return response.body().path("id").toString();
    }

    public String requestGeneration(String buildId, String jsonBody) {
        log.info("Requesting SBOM for build ID: {}, with jsonBody: {}", buildId, jsonBody);

        Response response = RestAssured.given()
                .baseUri(getSbomerBaseUri())
                .contentType(ContentType.JSON)
                .body(jsonBody)
                .when()
                .contentType(ContentType.JSON)
                .post(String.format("/api/v1alpha2/sboms/generate/build/%s", buildId));

        response.then()
                .statusCode(202)
                .body("buildId", CoreMatchers.is(buildId))
                .and()
                .body("status", CoreMatchers.is("NEW"));

        return response.body().path("id").toString();
    }
}
