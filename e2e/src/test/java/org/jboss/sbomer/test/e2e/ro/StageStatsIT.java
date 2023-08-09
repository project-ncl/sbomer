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
import org.junit.jupiter.api.Test;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;

@Tag("stage")
public class StageStatsIT extends E2EStageBase {
    private Response getStats() {
        return RestAssured.given()
                .baseUri(getSbomerBaseUri())
                .contentType(ContentType.JSON)
                .when()
                .get(String.format("/api/v1alpha1/stats"));
    }

    @Test
    public void testStats() throws IOException {
        Response stats = getStats();

        stats.then()
                .statusCode(200)
                .body("resources.sboms.total", CoreMatchers.is(Matchers.greaterThanOrEqualTo(0)))
                .body("resources.generationRequests.inProgress", CoreMatchers.is(Matchers.greaterThanOrEqualTo(0)))
                .body("resources.generationRequests.total", CoreMatchers.is(Matchers.greaterThanOrEqualTo(0)))
                .body("uptime", CoreMatchers.isA(String.class))
                .body("uptimeMillis", CoreMatchers.is(Matchers.greaterThan(0)))
                .body("version", CoreMatchers.isA(String.class));
    }

}
