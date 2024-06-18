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
package org.jboss.sbomer.service.test.integ.feature.sbom.umb.producer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.cyclonedx.model.Bom;
import org.jboss.sbomer.core.features.sbom.utils.SbomUtils;
import org.jboss.sbomer.service.feature.sbom.config.features.UmbConfig;
import org.jboss.sbomer.service.feature.sbom.features.umb.producer.AmqpMessageProducer;
import org.jboss.sbomer.service.feature.sbom.features.umb.producer.NotificationService;
import org.jboss.sbomer.service.feature.sbom.features.umb.producer.model.GenerationFinishedMessageBody;
import org.jboss.sbomer.service.feature.sbom.k8s.model.SbomGenerationStatus;
import org.jboss.sbomer.service.feature.sbom.model.Sbom;
import org.jboss.sbomer.service.feature.sbom.model.SbomGenerationRequest;
import org.jboss.sbomer.service.test.integ.feature.sbom.umb.producer.NotificationServiceIT.UmbProducerEnabled;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.vertx.core.json.JsonObject;
import jakarta.inject.Inject;

@QuarkusTest
@TestProfile(UmbProducerEnabled.class)
class NotificationServiceIT {

    static Path sbomPath(String fileName) {
        return Paths.get("src", "test", "resources", "sboms", fileName);
    }

    public static class UmbProducerEnabled implements QuarkusTestProfile {

        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                    "sbomer.features.umb.producer.enabled",
                    "true",
                    "sbomer.features.umb.producer.topic",
                    "a-test-topic");
        }
    }

    private Sbom createSBOM() throws IOException {
        Bom bom = SbomUtils.fromPath(NotificationServiceIT.sbomPath("sbom_with_errata.json"));

        SbomGenerationRequest generationRequest = SbomGenerationRequest.builder()
                .withId("AABB")
                .withIdentifier("BIDBID")
                .withStatus(SbomGenerationStatus.FINISHED)
                .build();

        Sbom sbom = new Sbom();
        sbom.setIdentifier("BIDBID");
        sbom.setRootPurl(bom.getMetadata().getComponent().getPurl());
        sbom.setId("416640206274228333");
        sbom.setSbom(SbomUtils.toJsonNode(bom));
        sbom.setGenerationRequest(generationRequest);
        return sbom;
    }

    @Inject
    UmbConfig umbConfig;

    @Inject
    NotificationService notificationService;

    @InjectMock
    AmqpMessageProducer amqpMessageProducer;

    @Test
    void shouldNotFailWhenNullIsPassed() {
        notificationService.notifyCompleted(null);
    }

    @Test
    void shouldNotFailWhenEmptyListIsPassed() {
        notificationService.notifyCompleted(Collections.emptyList());
    }

    @Test
    void shouldSuccessfullyNotify() throws IOException {
        ArgumentCaptor<GenerationFinishedMessageBody> argumentCaptor = ArgumentCaptor
                .forClass(GenerationFinishedMessageBody.class);

        notificationService.notifyCompleted(List.of(createSBOM(), createSBOM()));

        verify(amqpMessageProducer, times(2)).notify(argumentCaptor.capture());
        List<GenerationFinishedMessageBody> messages = argumentCaptor.getAllValues();

        JsonObject expected = JsonObject.of(
                "purl",
                "pkg:maven/com.github.michalszynkiewicz.test/empty@1.0.0.redhat-00271?type=jar",
                "productConfig",
                JsonObject.of(
                        "errataTool",
                        JsonObject.of(
                                "productName",
                                "RHTESTPRODUCT",
                                "productVersion",
                                "RHEL-8-RHTESTPRODUCT-1.1",
                                "productVariant",
                                "8Base-RHTESTPRODUCT-1.1")),
                "sbom",
                JsonObject.of(
                        "id",
                        "416640206274228333",
                        "link",
                        "http://localhost:8080/api/v1alpha2/sboms/416640206274228333",
                        "bom",
                        JsonObject.of(
                                "format",
                                "cyclonedx",
                                "version",
                                "1.4",
                                "link",
                                "http://localhost:8080/api/v1alpha2/sboms/416640206274228333/bom"),
                        "generationRequest",
                        JsonObject.of("id", "AABB")),
                "build",
                JsonObject.of(
                        "id",
                        "BIDBID",
                        "link",
                        "https://orch.psi.redhat.com/pnc-rest/v2/builds/AY2GVQCXDRQAA",
                        "system",
                        "pnc"));

        assertEquals(expected, JsonObject.mapFrom(messages.get(0)));
        assertEquals(expected, JsonObject.mapFrom(messages.get(1)));
    }

    @Test
    void shouldSkipNotificationForNonProductBuilds() throws IOException {
        Sbom sbom = createSBOM();

        // Remove all product properties
        sbom.setSbom(SbomUtils.removeErrataProperties(sbom.getSbom()));

        notificationService.notifyCompleted(List.of(sbom));

        verify(amqpMessageProducer, times(0)).notify(any(GenerationFinishedMessageBody.class));
    }
}
