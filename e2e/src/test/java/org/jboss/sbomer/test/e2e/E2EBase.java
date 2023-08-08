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
import io.restassured.specification.RequestSpecification;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class E2EBase {
    public abstract String sbomerUriPropertyName();

    public String getBaseUri() {
        String uri = System.getProperty(sbomerUriPropertyName());

        if (uri == null) {
            throw new RuntimeException(
                    String.format(
                            "Could not obtain SBOMer URI, make sure you set the '-D%s' property",
                            sbomerUriPropertyName()));
        }

        return uri;
    }

    public RequestSpecification given() {
        return RestAssured.given().baseUri(getBaseUri());
    }

    public String requestGeneration(String buildId) {
        log.info("Requesting SBOM for build ID: {}", buildId);

        Response response = given().when()
                .contentType(ContentType.JSON)
                .post(String.format("/api/v1alpha1/sboms/generate/build/%s", buildId));

        response.then()
                .statusCode(202)
                .body("buildId", CoreMatchers.is(buildId))
                .and()
                .body("status", CoreMatchers.is("NEW"));

        return response.body().path("id").toString();

    }
}
