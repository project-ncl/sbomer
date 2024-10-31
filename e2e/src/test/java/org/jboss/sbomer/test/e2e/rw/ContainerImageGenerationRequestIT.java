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
class ContainerImageGenerationRequestIT extends E2EStageBase {

    static Path sbomPath(String fileName) {
        return Paths.get("src", "test", "resources", "requests", fileName);
    }

    private final static String JWS_IMAGE = "registry.redhat.io/jboss-webserver-5/jws58-openjdk17-openshift-rhel8@sha256:f63b27a29c032843941b15567ebd1f37f540160e8066ac74c05367134c2ff3aa";
    private final static String MANDREL_IMAGE = "registry.redhat.io/quarkus/mandrel-for-jdk-21-rhel8@sha256:a406de0fd344785fb39eba81cbef01cf7fb3e2be43d0e671a8587d1abe1418b4";

    @Test
    void testSuccessfulGenerationForContainerImage() throws IOException, URISyntaxException {
        String requestBody = Files.readString(sbomPath("jws-image.json"));
        String generationRequestId = requestGenerationV1Beta1(requestBody);

        log.info("JWS container image - Generation Request created: {}", generationRequestId);

        waitForGeneration(generationRequestId);

        log.info("JWS container image finished, waiting for UMB message");

        publishedUmbMessage(generationRequestId, message -> {
            message.body("headers.generation_request_id", CoreMatchers.is(generationRequestId))
                    .body("headers.container_image", CoreMatchers.is(JWS_IMAGE))
                    .body("msg.sbom.generationRequest.id", CoreMatchers.is(generationRequestId))
                    .body("msg.sbom.generationRequest.type", CoreMatchers.is("CONTAINERIMAGE"))
                    .body("msg.sbom.generationRequest.containerimage.name", CoreMatchers.is(JWS_IMAGE));
        });

        log.info("JWS container image passed");
    }

    @Test
    void testMultiArchImage() throws IOException, URISyntaxException {
        String requestBody = Files.readString(sbomPath("mandrel-image.json"));
        String generationRequestId = requestGenerationV1Beta1(requestBody);

        log.info("Mandrel container image - Generation Request created: {}", generationRequestId);

        waitForGeneration(generationRequestId);

        log.info("Mandrel container image finished, waiting for UMB message");

        // TODO: Expect 4 messages
        publishedUmbMessage(generationRequestId, message -> {
            message.body("headers.generation_request_id", CoreMatchers.is(generationRequestId))
                    .body("headers.container_image", CoreMatchers.is(MANDREL_IMAGE))
                    .body("msg.sbom.generationRequest.id", CoreMatchers.is(generationRequestId))
                    .body("msg.sbom.generationRequest.type", CoreMatchers.is("CONTAINERIMAGE"))
                    .body("msg.sbom.generationRequest.containerimage.name", CoreMatchers.is(MANDREL_IMAGE));
        });

        final Response response = getSboms(generationRequestId);

        assertEquals(2, response.body().jsonPath().getInt("totalHits"));

        log.info("Mandrel container image passed");
    }
}
