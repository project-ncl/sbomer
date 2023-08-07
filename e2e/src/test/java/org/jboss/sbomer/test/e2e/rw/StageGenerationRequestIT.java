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
import org.jboss.sbomer.test.e2e.E2EStageBase;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Tag("stage")
public class StageGenerationRequestIT extends E2EStageBase {
    @Test
    public void testSuccessfulGeneration() throws IOException {
        String requestId = requestGeneration("AZ4HNIBW4YYAA");

        log.info("Generation Request created: {}", requestId);

        Awaitility.await().atMost(5, TimeUnit.MINUTES).pollInterval(5, TimeUnit.SECONDS).until(() -> {
            final Response body = given().contentType(ContentType.JSON)
                    .when()
                    .get(String.format("/api/v1alpha1/sboms/requests/%s", requestId));

            String status = body.path("status").toString();

            log.info("Current generation request status: {}", status);

            if (status.equals("FAILED")) {
                log.error("Generation failed: {}", body.asPrettyString());
                throw new Exception("Generation failed!");
            }

            return status.equals("FINISHED");
        });
    }
}
