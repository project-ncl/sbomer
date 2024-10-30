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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.hamcrest.CoreMatchers;
import org.jboss.sbomer.test.e2e.E2EStageBase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Execution(ExecutionMode.CONCURRENT)
class AdvisoryGenerationRequestIT extends E2EStageBase {

    static Path sbomPath(String fileName) {
        return Paths.get("src", "test", "resources", "requests", fileName);
    }

    private static final String ERRATA_QE_CONTAINER_IMAGE = "registry-proxy-stage.engineering.redhat.com/rh-osbs-stage/e2e-container-e2e-container-test-product@sha256:a7c041ff17c41f3f7b706159cc7e576f25b7012eae41898ee36074a0ff49e768";

    @Test
    void testContainerGenerationOfQEAdvisory() throws IOException, URISyntaxException {
        String requestBody = Files.readString(sbomPath("advisory-88484.json"));
        String generationRequestId = requestGenerationV1Beta1(requestBody);

        log.info("Advisory in QE status with Container - Generation Request created: {}", generationRequestId);

        waitForGeneration(generationRequestId);

        final Response response = getSboms(generationRequestId);

        response.then()
                .log()
                .all()
                .statusCode(200)
                .body("content[0].identifier", CoreMatchers.is(ERRATA_QE_CONTAINER_IMAGE));

        log.info("Advisory in QE status with Container generated!");
    }

    // private static final String ERRATA_ID_QE_RPM = "89769"; // RHBA-2024:89769-01
    // private static final String ERRATA_RPM_NVR = "redhat-release-computenode-7.2-8.el7_2.1"; // id=561861
    @Test
    void testRPMGenerationOfQEAdvisory() throws IOException, URISyntaxException {
        String requestBody = Files.readString(sbomPath("advisory-89769.json"));
        String generationRequestId = requestGenerationV1Beta1(requestBody);

        log.info("Advisory in QE status with RPMs - Generation Request created: {}", generationRequestId);

        waitForGeneration(generationRequestId);

        final Response response = getSboms(generationRequestId);
        assertEquals(1, response.body().jsonPath().getInt("totalHits"));

        log.info("Advisory in QE status with Container generated!");
    }

}
