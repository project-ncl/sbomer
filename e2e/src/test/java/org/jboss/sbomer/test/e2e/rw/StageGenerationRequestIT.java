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
package org.jboss.sbomer.test.e2e.rw;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import org.awaitility.Awaitility;
import org.hamcrest.CoreMatchers;
import org.jboss.sbomer.test.e2e.E2EStageBase;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Tag("stage")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class StageGenerationRequestIT extends E2EStageBase {

    static Path sbomPath(String fileName) {
        return Paths.get("src", "test", "resources", "requests", fileName);
    }

    private final static String MAVEN_BUILD_ID = "AZ4HNIBW4YYAA";
    private final static String GRADLE_5_BUILD_ID = "A3YCIKLQTVYAA";
    private final static String GRADLE_4_BUILD_ID = "A3LCEFCLLVYAA";

    static String mavenGenerationRequestId;
    static String gradle5GenerationRequestId;
    static String gradle4GenerationRequestId;

    @Test
    @Order(1)
    public void testSuccessfulGenerationMavenBuild() throws IOException {
        mavenGenerationRequestId = requestGeneration(MAVEN_BUILD_ID);

        log.info("Maven build - Generation Request created: {}", mavenGenerationRequestId);

        Awaitility.await().atMost(10, TimeUnit.MINUTES).pollInterval(5, TimeUnit.SECONDS).until(() -> {
            final Response body = getGeneration(mavenGenerationRequestId);
            String status = body.path("status").toString();

            log.info("Maven build - Current generation request status: {}", status);

            if (status.equals("FAILED")) {
                log.error("Maven build - Generation failed: {}", body.asPrettyString());
                throw new Exception("Maven build - Generation failed!");
            }

            return status.equals("FINISHED");
        });
    }

    @Test
    @Order(2)
    public void ensureUmbMessageWasSentForMavenBuild() {
        Awaitility.await().atMost(2, TimeUnit.MINUTES).pollInterval(5, TimeUnit.SECONDS).until(() -> {
            givenLastCompleteUmbMessageForGeneration(mavenGenerationRequestId).then()
                    .body("raw_messages[0].headers.generation_request_id", CoreMatchers.is(mavenGenerationRequestId))
                    .body("raw_messages[0].headers.pnc_build_id", CoreMatchers.is(MAVEN_BUILD_ID))
                    .body("raw_messages[0].msg.build.id", CoreMatchers.is(MAVEN_BUILD_ID))
                    .body("raw_messages[0].msg.sbom.generationRequest.id", CoreMatchers.is(mavenGenerationRequestId));

            return true;
        });
    }

    @Test
    @Order(3)
    public void testSuccessfulGenerationGradle5Build() throws IOException {
        String requestBody = Files.readString(sbomPath(GRADLE_5_BUILD_ID + ".json"));
        gradle5GenerationRequestId = requestGenerationWithConfiguration(GRADLE_5_BUILD_ID, requestBody);

        log.info("Gradle 5 build - Generation Request created: {}", gradle5GenerationRequestId);

        Awaitility.await().atMost(10, TimeUnit.MINUTES).pollInterval(5, TimeUnit.SECONDS).until(() -> {
            final Response body = getGeneration(gradle5GenerationRequestId);
            String status = body.path("status").toString();

            log.info("Gradle 5 build - Current generation request status: {}", status);

            if (status.equals("FAILED")) {
                log.error("Gradle 5 build - generation failed: {}", body.asPrettyString());
                throw new Exception("Gradle 5 build - Generation failed!");
            }

            return status.equals("FINISHED");
        });
    }

    @Test
    @Order(4)
    public void ensureUmbMessageWasSentForGradle5Build() {
        Awaitility.await().atMost(2, TimeUnit.MINUTES).pollInterval(5, TimeUnit.SECONDS).until(() -> {
            givenLastCompleteUmbMessageForGeneration(gradle5GenerationRequestId).then()
                    .body("raw_messages[0].headers.generation_request_id", CoreMatchers.is(gradle5GenerationRequestId))
                    .body("raw_messages[0].headers.pnc_build_id", CoreMatchers.is(GRADLE_5_BUILD_ID))
                    .body("raw_messages[0].msg.build.id", CoreMatchers.is(GRADLE_5_BUILD_ID))
                    .body("raw_messages[0].msg.sbom.generationRequest.id", CoreMatchers.is(gradle5GenerationRequestId));

            return true;
        });
    }

    @Test
    @Order(5)
    public void testSuccessfulGenerationGradle4Build() throws IOException {
        String requestBody = Files.readString(sbomPath(GRADLE_4_BUILD_ID + ".json"));
        gradle4GenerationRequestId = requestGenerationWithConfiguration(GRADLE_4_BUILD_ID, requestBody);

        log.info("Gradle 4 build - Generation Request created: {}", gradle4GenerationRequestId);

        Awaitility.await().atMost(10, TimeUnit.MINUTES).pollInterval(5, TimeUnit.SECONDS).until(() -> {
            final Response body = getGeneration(gradle4GenerationRequestId);
            String status = body.path("status").toString();

            log.info("Gradle 4 build - Current generation request status: {}", status);

            if (status.equals("FAILED")) {
                log.error("Gradle 4 build - generation failed: {}", body.asPrettyString());
                throw new Exception("Gradle 4 build - Generation failed!");
            }

            return status.equals("FINISHED");
        });
    }

    // @Test
    @Order(6)
    public void ensureUmbMessageWasSentForGradle4Build() {
        Awaitility.await().atMost(2, TimeUnit.MINUTES).pollInterval(5, TimeUnit.SECONDS).until(() -> {
            givenLastCompleteUmbMessageForGeneration(gradle4GenerationRequestId).then()
                    .body("raw_messages[0].headers.generation_request_id", CoreMatchers.is(gradle4GenerationRequestId))
                    .body("raw_messages[0].headers.pnc_build_id", CoreMatchers.is(GRADLE_4_BUILD_ID))
                    .body("raw_messages[0].msg.build.id", CoreMatchers.is(GRADLE_4_BUILD_ID))
                    .body("raw_messages[0].msg.sbom.generationRequest.id", CoreMatchers.is(gradle4GenerationRequestId));

            return true;
        });
    }
}
