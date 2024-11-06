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
import java.util.List;

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

    private final static String MANDREL_IMAGE = "registry.redhat.io/quarkus/mandrel-for-jdk-21-rhel8@sha256:a406de0fd344785fb39eba81cbef01cf7fb3e2be43d0e671a8587d1abe1418b4";

    @Test
    void testMultiArchImage() throws IOException, URISyntaxException {
        String requestBody = Files.readString(sbomPath("mandrel-image.json"));
        List<String> generationIds = requestGeneration(requestBody);

        assertEquals(1, generationIds.size());

        String generationId = generationIds.get(0);

        log.info("Mandrel container image - Generation Request created: {}", generationId);

        waitForGeneration(generationId);

        // TODO: UMB check disabled, because we do not add product coordinates anymore which control if we should send
        // UMB message
        // log.info("Mandrel container image finished, waiting for UMB message");

        // // TODO: Expect 4 messages
        // publishedUmbMessage(generationId, message -> {
        // message.body("headers.generation_request_id", CoreMatchers.is(generationId))
        // .body("headers.container_image", CoreMatchers.is(MANDREL_IMAGE))
        // .body("msg.sbom.generationRequest.id", CoreMatchers.is(generationId))
        // .body("msg.sbom.generationRequest.type", CoreMatchers.is("CONTAINERIMAGE"))
        // .body("msg.sbom.generationRequest.containerimage.name", CoreMatchers.is(MANDREL_IMAGE));
        // });

        final Response response = getManifestsForGeneration(generationId);

        assertEquals(2, response.body().jsonPath().getInt("totalHits"));

        log.info("Mandrel container image passed");
    }
}
