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

import org.hamcrest.CoreMatchers;
import org.jboss.sbomer.test.e2e.E2EStageBase;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Tag("stage")
@Execution(ExecutionMode.CONCURRENT)
class StageGenerationRequestIT extends E2EStageBase {

    static Path sbomPath(String fileName) {
        return Paths.get("src", "test", "resources", "requests", fileName);
    }

    private final static String MAVEN_BUILD_ID = "AZ4HNIBW4YYAA";
    private final static String GRADLE_5_BUILD_ID = "A3YCIKLQTVYAA";
    private final static String GRADLE_4_BUILD_ID = "A3LCEFCLLVYAA";
    private final static String NODEJS_NPM_BUILD_ID = "A4WLAOY3BJIAA";

    @Test
    void testSuccessfulGenerationMavenBuild() throws IOException {
        String generationRequestId = requestGeneration(MAVEN_BUILD_ID);

        log.info("Maven build - Generation Request created: {}", generationRequestId);

        waitForGeneration(generationRequestId);

        log.info("Maven build finished, waiting for UMB message");

        lastCompleteUmbMessageResponse(generationRequestId, resp -> {
            resp.then()
                    .body("raw_messages[0].headers.generation_request_id", CoreMatchers.is(generationRequestId))
                    .body("raw_messages[0].headers.pnc_build_id", CoreMatchers.is(MAVEN_BUILD_ID))
                    .body("raw_messages[0].msg.build.id", CoreMatchers.is(MAVEN_BUILD_ID))
                    .body("raw_messages[0].msg.sbom.generationRequest.id", CoreMatchers.is(generationRequestId));
        });

        log.info("Maven build passed");
    }

    @Test
    void testSuccessfulGenerationGradle5Build() throws IOException {
        String requestBody = Files.readString(sbomPath(GRADLE_5_BUILD_ID + ".json"));
        String generationRequestId = requestGenerationWithConfiguration(GRADLE_5_BUILD_ID, requestBody);

        log.info("Gradle 5 build - Generation Request created: {}", generationRequestId);

        waitForGeneration(generationRequestId);

        log.info("Gradle 5 build finished, waiting for UMB message");

        lastCompleteUmbMessageResponse(generationRequestId, resp -> {
            resp.then()
                    .body("raw_messages[0].headers.generation_request_id", CoreMatchers.is(generationRequestId))
                    .body("raw_messages[0].headers.pnc_build_id", CoreMatchers.is(GRADLE_5_BUILD_ID))
                    .body("raw_messages[0].msg.build.id", CoreMatchers.is(GRADLE_5_BUILD_ID))
                    .body("raw_messages[0].msg.sbom.generationRequest.id", CoreMatchers.is(generationRequestId));
        });

        log.info("Gradle 5 build passed");
    }

    @Test
    void testSuccessfulGenerationGradle4Build() throws IOException {
        String requestBody = Files.readString(sbomPath(GRADLE_4_BUILD_ID + ".json"));
        String generationRequestId = requestGenerationWithConfiguration(GRADLE_4_BUILD_ID, requestBody);

        log.info("Gradle 4 build - Generation Request created: {}", generationRequestId);

        waitForGeneration(generationRequestId);

        // log.info("Gradle 4 build finished, waiting for UMB message");

        // Awaitility.await().atMost(2, TimeUnit.MINUTES).pollInterval(5, TimeUnit.SECONDS).until(() -> {
        // givenLastCompleteUmbMessageForGeneration(gradle4GenerationRequestId).then()
        // .body("raw_messages[0].headers.generation_request_id", CoreMatchers.is(gradle4GenerationRequestId))
        // .body("raw_messages[0].headers.pnc_build_id", CoreMatchers.is(GRADLE_4_BUILD_ID))
        // .body("raw_messages[0].msg.build.id", CoreMatchers.is(GRADLE_4_BUILD_ID))
        // .body("raw_messages[0].msg.sbom.generationRequest.id", CoreMatchers.is(gradle4GenerationRequestId));

        // return true;
        // });

        log.info("Gradle 4 build passed");
    }

    @Test
    void testSuccessfulGenerationNodeJsNpmBuild() throws IOException {
        String requestBody = Files.readString(sbomPath(NODEJS_NPM_BUILD_ID + ".json"));
        String generationRequestId = requestGenerationWithConfiguration(NODEJS_NPM_BUILD_ID, requestBody);

        log.info("NodeJs NPM build - Generation Request created: {}", generationRequestId);

        waitForGeneration(generationRequestId);

        log.info("NodeJs NPM build finished, waiting for UMB message");

        lastCompleteUmbMessageResponse(generationRequestId, resp -> {
            resp.then()
                    .body("raw_messages[0].headers.generation_request_id", CoreMatchers.is(generationRequestId))
                    .body("raw_messages[0].headers.pnc_build_id", CoreMatchers.is(NODEJS_NPM_BUILD_ID))
                    .body("raw_messages[0].msg.build.id", CoreMatchers.is(NODEJS_NPM_BUILD_ID))
                    .body("raw_messages[0].msg.sbom.generationRequest.id", CoreMatchers.is(generationRequestId));
        });

        log.info("NodeJs NPM build passed");
    }
}
