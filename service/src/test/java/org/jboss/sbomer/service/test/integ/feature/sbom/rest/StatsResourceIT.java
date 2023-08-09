/*
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
package org.jboss.sbomer.service.test.integ.feature.sbom.rest;

import org.hamcrest.CoreMatchers;
import org.jboss.sbomer.service.feature.sbom.service.SbomService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectSpy;
import io.quarkus.test.kubernetes.client.WithKubernetesTestServer;
import io.restassured.RestAssured;

@QuarkusTest
@WithKubernetesTestServer
public class StatsResourceIT {

    @InjectSpy
    SbomService sbomService;

    @Test
    void testEmptyStatsEndpoint() {
        RestAssured.given()
                .when()
                .get("/api/v1alpha1/stats")
                .then()
                .statusCode(200)
                .body("resources.sboms.total", CoreMatchers.is(0))
                .body("resources.generationRequests.total", CoreMatchers.is(0))
                .body("resources.generationRequests.inProgress", CoreMatchers.is(0))
                .body("uptime", CoreMatchers.isA(String.class))
                .body("uptimeMillis", CoreMatchers.isA(Integer.class))
                .body("version", CoreMatchers.isA(String.class));
    }

    @Test
    void testStatsEndpoint() {
        Mockito.when(sbomService.countSboms()).thenReturn(12l);
        Mockito.when(sbomService.countSbomGenerationRequests()).thenReturn(500l);

        RestAssured.given()
                .when()
                .get("/api/v1alpha1/stats")
                .then()
                .statusCode(200)
                .body("resources.sboms.total", CoreMatchers.is(12))
                .body("resources.generationRequests.total", CoreMatchers.is(500));
    }

}
