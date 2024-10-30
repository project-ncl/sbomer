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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

import org.cyclonedx.model.Bom;
import org.jboss.sbomer.core.errors.ApplicationException;
import org.jboss.sbomer.core.features.sbom.enums.GenerationRequestType;
import org.jboss.sbomer.core.features.sbom.utils.SbomUtils;
import org.jboss.sbomer.service.feature.FeatureFlags;
import org.jboss.sbomer.service.feature.sbom.config.features.UmbConfig;
import org.jboss.sbomer.service.feature.sbom.features.umb.producer.AmqpMessageProducer;
import org.jboss.sbomer.service.feature.sbom.features.umb.producer.NotificationService;
import org.jboss.sbomer.service.feature.sbom.features.umb.producer.model.GenerationFinishedMessageBody;
import org.jboss.sbomer.service.feature.sbom.k8s.model.SbomGenerationStatus;
import org.jboss.sbomer.service.feature.sbom.model.Sbom;
import org.jboss.sbomer.service.feature.sbom.model.SbomGenerationRequest;
import org.jboss.sbomer.service.test.utils.umb.TestUmbProfile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.vertx.core.json.JsonObject;
import jakarta.inject.Inject;

@QuarkusTest
@TestProfile(TestUmbProfile.class)
class NotificationServiceIT {

    @InjectMock
    FeatureFlags featureFlags;

    static Path sbomPath(String fileName) {
        return Paths.get("src", "test", "resources", "sboms", fileName);
    }

    private Sbom createOperationSBOM() throws IOException {
        Bom bom = SbomUtils.fromPath(NotificationServiceIT.sbomPath("complete_operation_sbom.json"));

        SbomGenerationRequest generationRequest = SbomGenerationRequest.builder()
                .withId("AABB")
                .withType(GenerationRequestType.OPERATION)
                .withIdentifier("OPID")
                .withStatus(SbomGenerationStatus.FINISHED)
                .build();

        Sbom sbom = new Sbom();
        sbom.setIdentifier("OPID");
        sbom.setRootPurl(bom.getMetadata().getComponent().getPurl());
        sbom.setId("416640206274228333");
        sbom.setSbom(SbomUtils.toJsonNode(bom));
        sbom.setGenerationRequest(generationRequest);
        return sbom;
    }

    private Sbom createSBOM() throws IOException {
        Bom bom = SbomUtils.fromPath(NotificationServiceIT.sbomPath("sbom_with_errata.json"));

        SbomGenerationRequest generationRequest = SbomGenerationRequest.builder()
                .withId("AABB")
                .withType(GenerationRequestType.BUILD)
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

    private Sbom createMinimalRpmManifest() throws IOException {
        Bom bom = SbomUtils.fromPath(NotificationServiceIT.sbomPath("minimal-rpm.json"));

        SbomGenerationRequest generationRequest = SbomGenerationRequest.builder()
                .withId("AABB")
                .withType(GenerationRequestType.BREW_RPM)
                .withIdentifier("1234")
                .withStatus(SbomGenerationStatus.FINISHED)
                .build();

        Sbom sbom = new Sbom();
        sbom.setIdentifier("SBOMIDRPM");
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
    void shouldSuccessfullyNotifyForContainerImage() throws IOException {
        Mockito.when(featureFlags.shouldNotify(eq(GenerationRequestType.CONTAINERIMAGE))).thenReturn(true);

        ArgumentCaptor<GenerationFinishedMessageBody> argumentCaptor = ArgumentCaptor
                .forClass(GenerationFinishedMessageBody.class);

        // Reusing the manifest for a PNC build, doesn't matter
        Sbom sbom = createSBOM();
        sbom.getGenerationRequest().setType(GenerationRequestType.CONTAINERIMAGE);
        sbom.getGenerationRequest().setIdentifier("registry/a-container-image");

        notificationService.notifyCompleted(List.of(sbom));

        verify(amqpMessageProducer, times(1)).notify(argumentCaptor.capture());
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
                        "http://localhost:8080/api/v1alpha3/sboms/416640206274228333",
                        "bom",
                        JsonObject.of(
                                "format",
                                "cyclonedx",
                                "version",
                                "1.4",
                                "link",
                                "http://localhost:8080/api/v1alpha3/sboms/416640206274228333/bom"),
                        "generationRequest",
                        JsonObject.of(
                                "id",
                                "AABB",
                                "type",
                                "CONTAINERIMAGE",
                                "containerimage",
                                JsonObject.of("name", "registry/a-container-image"))));

        assertEquals(expected, JsonObject.mapFrom(messages.get(0)));
    }

    @Test
    void shouldGracefullySkipNotifyForMinimalBrewRpm() throws IOException {
        Sbom sbom = createMinimalRpmManifest();
        notificationService.notifyCompleted(List.of(sbom));
        verify(amqpMessageProducer, times(0)).notify(any(GenerationFinishedMessageBody.class));
    }

    @Test
    void shouldSuccessfullyNotifyForBuild() throws IOException {
        Mockito.when(featureFlags.shouldNotify(eq(GenerationRequestType.BUILD))).thenReturn(true);

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
                        "http://localhost:8080/api/v1alpha3/sboms/416640206274228333",
                        "bom",
                        JsonObject.of(
                                "format",
                                "cyclonedx",
                                "version",
                                "1.4",
                                "link",
                                "http://localhost:8080/api/v1alpha3/sboms/416640206274228333/bom"),
                        "generationRequest",
                        JsonObject.of(
                                "id",
                                "AABB",
                                "type",
                                "BUILD",
                                "build",
                                JsonObject.of(
                                        "id",
                                        "BIDBID",
                                        "link",
                                        "https://orch.psi.redhat.com/pnc-rest/v2/builds/AY2GVQCXDRQAA",
                                        "system",
                                        "pnc"))),
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
    void shouldSuccessfullyNotifyForOperation() throws IOException {
        Mockito.when(featureFlags.shouldNotify(eq(GenerationRequestType.OPERATION))).thenReturn(true);

        ArgumentCaptor<GenerationFinishedMessageBody> argumentCaptor = ArgumentCaptor
                .forClass(GenerationFinishedMessageBody.class);

        notificationService.notifyCompleted(List.of(createOperationSBOM()));

        verify(amqpMessageProducer, times(1)).notify(argumentCaptor.capture());

        List<GenerationFinishedMessageBody> messages = argumentCaptor.getAllValues();

        JsonObject expected = JsonObject.of(
                "purl",
                "pkg:generic/my-broker-7.11.5.CR3-bin.zip@7.11.5.CR3?operation=A5RPHL7Y3AIAA",
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
                        "http://localhost:8080/api/v1alpha3/sboms/416640206274228333",
                        "bom",
                        JsonObject.of(
                                "format",
                                "cyclonedx",
                                "version",
                                "1.4",
                                "link",
                                "http://localhost:8080/api/v1alpha3/sboms/416640206274228333/bom"),
                        "generationRequest",
                        JsonObject.of(
                                "id",
                                "AABB",
                                "type",
                                "OPERATION",
                                "operation",
                                JsonObject.of(
                                        "id",
                                        "OPID",
                                        "link",
                                        "http://orch.com/pnc-rest/v2/operations/deliverable-analyzer/A5RPHL7Y3AIAA",
                                        "system",
                                        "pnc",
                                        "deliverable",
                                        "7.11.5.CR3"))),
                "operation",
                JsonObject.of(
                        "id",
                        "OPID",
                        "link",
                        "http://orch.com/pnc-rest/v2/operations/deliverable-analyzer/A5RPHL7Y3AIAA",
                        "system",
                        "pnc",
                        "deliverable",
                        "7.11.5.CR3"));

        assertEquals(expected, JsonObject.mapFrom(messages.get(0)));
    }

    @Test
    void shouldSkipNotificationForNonProductBuilds() throws IOException {
        Sbom sbom = createSBOM();

        // Remove all product properties
        sbom.setSbom(SbomUtils.removeErrataProperties(sbom.getSbom()));

        notificationService.notifyCompleted(List.of(sbom));

        verify(amqpMessageProducer, times(0)).notify(any(GenerationFinishedMessageBody.class));
    }

    @ParameterizedTest
    @EnumSource(GenerationRequestType.class)
    void shouldSkipNotificationIfTypeIsDisabled(GenerationRequestType type) throws IOException {
        Mockito.when(featureFlags.shouldNotify(eq(type))).thenReturn(false);

        Sbom sbom = createSBOM();
        sbom.getGenerationRequest().setType(type);

        ApplicationException ex = assertThrows(ApplicationException.class, () -> {
            notificationService.notifyCompleted(List.of(sbom));
        });

        assertEquals(
                "Notifications for '" + type.toString() + "' type are disabled, notification service won't send it",
                ex.getMessage());

        verify(amqpMessageProducer, times(0)).notify(any(GenerationFinishedMessageBody.class));
    }
}
