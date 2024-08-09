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
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.hamcrest.CoreMatchers;
import org.jboss.sbomer.test.e2e.E2EStageBase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Execution(ExecutionMode.CONCURRENT)
class ContainerImageGenerationRequestIT extends E2EStageBase {

    static Path sbomPath(String fileName) {
        return Paths.get("src", "test", "resources", "requests", fileName);
    }

    private final static String JWS_IMAGE = "registry.redhat.io/jboss-webserver-5/jws58-openjdk17-openshift-rhel8@sha256:f63b27a29c032843941b15567ebd1f37f540160e8066ac74c05367134c2ff3aa";

    @Test
    void testSuccessfulGenerationForContainerImage() throws IOException, URISyntaxException {
        String requestBody = Files.readString(sbomPath("jws-image.json"));
        String generationRequestId = requestContainerImageGeneration(JWS_IMAGE, requestBody);

        log.info("JWS container image - Generation Request created: {}", generationRequestId);

        waitForGeneration(generationRequestId);

        log.info("JWS container image finished, waiting for UMB message");

        lastCompleteUmbMessageResponse(generationRequestId, resp -> {
            resp.then()
                    .body("raw_messages[0].headers.generation_request_id", CoreMatchers.is(generationRequestId))
                    .body("raw_messages[0].headers.container_image", CoreMatchers.is(JWS_IMAGE))
                    .body("raw_messages[0].msg.sbom.generationRequest.id", CoreMatchers.is(generationRequestId))
                    .body("raw_messages[0].msg.sbom.generationRequest.type", CoreMatchers.is("CONTAINERIMAGE"))
                    .body("raw_messages[0].msg.sbom.generationRequest.containerimage.name", CoreMatchers.is(JWS_IMAGE));
        });

        log.info("JWS container image passed");
    }
}
