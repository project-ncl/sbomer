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
import java.util.concurrent.TimeUnit;

import org.awaitility.Awaitility;
import org.hamcrest.CoreMatchers;
import org.jboss.sbomer.test.e2e.E2EStageBase;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Tag("stage")
public class StageGenerationRequestIT extends E2EStageBase {

    private final static String BUILD_ID = "AZ4HNIBW4YYAA";
    static String generationRequestId;

    @Test
    @Order(1)
    public void testSuccessfulGeneration() throws IOException {
        generationRequestId = requestGeneration(BUILD_ID);

        log.info("Generation Request created: {}", generationRequestId);

        Awaitility.await().atMost(10, TimeUnit.MINUTES).pollInterval(5, TimeUnit.SECONDS).until(() -> {
            final Response body = getGeneration(generationRequestId);
            String status = body.path("status").toString();

            log.info("Current generation request status: {}", status);

            if (status.equals("FAILED")) {
                log.error("Generation failed: {}", body.asPrettyString());
                throw new Exception("Generation failed!");
            }

            return status.equals("FINISHED");
        });
    }

    @Test
    @Order(2)
    public void ensureUmbMessageWasSent() {
        Awaitility.await().atMost(2, TimeUnit.MINUTES).pollInterval(5, TimeUnit.SECONDS).until(() -> {
            givenLastCompleteUmbMessage().then()
                    .body("raw_messages[0].headers.generation_request_id", CoreMatchers.is(generationRequestId))
                    .body("raw_messages[0].headers.pnc_build_id", CoreMatchers.is(BUILD_ID))
                    .body("raw_messages[0].msg.build.id", CoreMatchers.is(BUILD_ID))
                    .body("raw_messages[0].msg.sbom.generationRequest.id", CoreMatchers.is(generationRequestId));

            return true;
        });
    }
}
