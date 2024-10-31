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
package org.jboss.sbomer.test.e2e.ro;

import java.io.IOException;

import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;
import org.jboss.sbomer.test.e2e.E2EStageBase;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;

@Tag("stage")
class StageStatsIT extends E2EStageBase {
    private Response getStats(String apiVersion) {
        return RestAssured.given()
                .baseUri(getSbomerBaseUri())
                .contentType(ContentType.JSON)
                .when()
                .get(String.format("/api/%s/stats", apiVersion));
    }

    @ParameterizedTest
    @ValueSource(strings = { "v1alpha3", "v1beta1" })
    void testStats(String version) throws IOException {
        Response stats = getStats(version);

        stats.then()
                .statusCode(200)
                .body("resources.sboms.total", CoreMatchers.is(Matchers.greaterThanOrEqualTo(0)))
                .body("resources.generationRequests.inProgress", CoreMatchers.is(Matchers.greaterThanOrEqualTo(0)))
                .body("resources.generationRequests.total", CoreMatchers.is(Matchers.greaterThanOrEqualTo(0)))
                .body("uptime", CoreMatchers.isA(String.class))
                .body("uptimeMillis", CoreMatchers.is(Matchers.greaterThan(0)))
                .body("version", CoreMatchers.isA(String.class))
                .body("appEnv", CoreMatchers.is("stage"))
                .body("deployment.target", CoreMatchers.is("aws"))
                .body("deployment.type", CoreMatchers.is("preprod"))
                .body("deployment.zone", CoreMatchers.is("us-east-1"))
                .body("release", CoreMatchers.is("sbomer"));
    }
}
