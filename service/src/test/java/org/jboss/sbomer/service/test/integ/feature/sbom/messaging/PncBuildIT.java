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
package org.jboss.sbomer.service.test.integ.feature.sbom.messaging;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.awaitility.Awaitility;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.spi.Connector;
import org.jboss.sbomer.core.features.sbom.enums.GenerationRequestType;
import org.jboss.sbomer.core.features.sbom.enums.GenerationResult;
import org.jboss.sbomer.core.features.sbom.utils.ObjectMapperProvider;
import org.jboss.sbomer.core.test.TestResources;
import org.jboss.sbomer.service.feature.sbom.features.umb.consumer.AmqpMessageConsumer;
import org.jboss.sbomer.service.feature.sbom.features.umb.consumer.PncNotificationHandler;
import org.jboss.sbomer.service.feature.sbom.features.umb.consumer.model.PncBuildNotificationMessageBody;
import org.jboss.sbomer.service.feature.sbom.features.umb.consumer.model.PncDelAnalysisNotificationMessageBody;
import org.jboss.sbomer.service.feature.sbom.features.umb.producer.AmqpMessageProducer;
import org.jboss.sbomer.service.feature.sbom.k8s.model.GenerationRequest;
import org.jboss.sbomer.service.feature.sbom.k8s.model.SbomGenerationStatus;
import org.jboss.sbomer.service.feature.sbom.model.SbomGenerationRequest;
import org.jboss.sbomer.service.feature.sbom.service.SbomGenerationRequestRepository;
import org.jboss.sbomer.service.test.PncWireMock;
import org.jboss.sbomer.service.test.utils.AmqpMessageHelper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.quarkus.test.common.WithTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectSpy;
import io.quarkus.test.kubernetes.client.WithKubernetesTestServer;
import io.smallrye.reactive.messaging.memory.InMemoryConnector;
import io.smallrye.reactive.messaging.memory.InMemorySource;
import io.vertx.core.json.JsonObject;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@QuarkusTest
@WithTestResource(AmqpTestResourceLifecycleManager.class)
@WithTestResource(PncWireMock.class)
@Slf4j
@WithKubernetesTestServer
class PncBuildIT {

    @Inject
    AmqpMessageConsumer consumer;

    @Inject
    AmqpMessageProducer producer;

    @InjectSpy
    PncNotificationHandler handler;

    @Inject
    KubernetesClient kubernetesClient;

    @Inject
    SbomGenerationRequestRepository sbomGenerationRequestRepository;

    @Inject
    @Connector("smallrye-in-memory")
    InMemoryConnector connector;

    @Test
    void testUMBProducerPNCBuild() throws Exception {
        log.info("Running testUMBProducerPNCBuild...");

        InMemorySource<Message<String>> builds = connector.source("builds");
        Message<String> txgMsg = preparePNCBuildMsg();

        builds.send(txgMsg);

        ArgumentCaptor<Message<String>> msgArgumentCaptor = ArgumentCaptor.forClass(Message.class);
        ArgumentCaptor<GenerationRequestType> msgTypeArgumentCaptor = ArgumentCaptor
                .forClass(GenerationRequestType.class);

        Awaitility.await().atMost(30, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(() -> {
            List<ConfigMap> configMaps = kubernetesClient.configMaps().list().getItems();

            Optional<ConfigMap> request = configMaps.stream()
                    .filter(cm -> cm.getData().get(GenerationRequest.KEY_IDENTIFIER).equals("AX5TJMYHQAIAE"))
                    .findFirst();

            if (request.isPresent()) {
                log.info("Generation request was found!");
                return true;
            }

            return false;
        });

        verify(handler, times(1)).handle(msgArgumentCaptor.capture(), msgTypeArgumentCaptor.capture());
        List<Message<String>> messages = msgArgumentCaptor.getAllValues();
        List<GenerationRequestType> messageTypes = msgTypeArgumentCaptor.getAllValues();

        assertEquals(1, messages.size());
        assertEquals(1, messageTypes.size());
        assertEquals(GenerationRequestType.BUILD, messageTypes.get(0));

        PncBuildNotificationMessageBody buildMsgBody = ObjectMapperProvider.json()
                .readValue(String.valueOf(messages.get(0).getPayload()), PncBuildNotificationMessageBody.class);

        // See "payloads/umb-pnc-build-body.json" file
        assertEquals(buildMsgBody.getBuild().getId(), "AX5TJMYHQAIAE");
    }

    @Test
    void testUMBProducerDelAnalysisOperation() throws Exception {
        log.info("Running testUMBProducerDelAnalysisOperation...");

        InMemorySource<Message<String>> builds = connector.source("builds");
        Message<String> txgMsg = preparePNCDelAnalysisMsg();

        builds.send(txgMsg);

        ArgumentCaptor<Message<String>> msgArgumentCaptor = ArgumentCaptor.forClass(Message.class);
        ArgumentCaptor<GenerationRequestType> msgTypeArgumentCaptor = ArgumentCaptor
                .forClass(GenerationRequestType.class);

        Awaitility.await().atMost(30, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(() -> {
            List<ConfigMap> configMaps = kubernetesClient.configMaps().list().getItems();

            Optional<ConfigMap> request = configMaps.stream()
                    .filter(cm -> cm.getData().get(GenerationRequest.KEY_IDENTIFIER).equals("A6DFVW2SACIAA"))
                    .findFirst();

            if (request.isPresent()) {
                log.info("Generation request was found!");
                return true;
            }

            return false;
        });

        verify(handler, times(1)).handle(msgArgumentCaptor.capture(), msgTypeArgumentCaptor.capture());
        List<Message<String>> messages = msgArgumentCaptor.getAllValues();
        List<GenerationRequestType> messageTypes = msgTypeArgumentCaptor.getAllValues();

        assertEquals(1, messages.size());
        assertEquals(1, messageTypes.size());
        assertEquals(GenerationRequestType.OPERATION, messageTypes.get(0));

        PncDelAnalysisNotificationMessageBody buildMsgBody = ObjectMapperProvider.json()
                .readValue(String.valueOf(messages.get(0).getPayload()), PncDelAnalysisNotificationMessageBody.class);

        // See "payloads/umb-pnc-build-body.json" file
        assertEquals(buildMsgBody.getOperationId(), "A6DFVW2SACIAA");
        assertEquals(buildMsgBody.getMilestoneId(), "2712");
    }

    @Test
    public void testUMBConsumerFailedDelAnalysisOperation() throws Exception {
        log.info("Running testUMBConsumerFailedDelAnalysisOperation...");

        // Prepare initial NO_OP request.
        SbomGenerationRequest request = SbomGenerationRequest.builder()
                .withId("FAILEDOPERATION")
                .withIdentifier("BAQZMQOS7YIAA")
                .withType(GenerationRequestType.OPERATION)
                .withStatus(SbomGenerationStatus.NO_OP)
                .build();

        sbomGenerationRequestRepository.save(request);

        InMemorySource<Message<String>> builds = connector.source("builds");
        Message<String> txgMsg = prepareFailedPNCDelAnalysisMsg();

        // Send the message to notify that the build failed.
        builds.send(txgMsg);

        Awaitility.await().atMost(5, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(() -> {
            SbomGenerationRequest updated = sbomGenerationRequestRepository.findById("FAILEDOPERATION");

            if (updated != null && updated.getStatus().equals(SbomGenerationStatus.FAILED)) {
                return true;
            }

            return false;
        });

        SbomGenerationRequest updatedRequest = sbomGenerationRequestRepository.findById("FAILEDOPERATION");

        assertEquals(SbomGenerationStatus.FAILED, updatedRequest.getStatus());
        assertEquals("Deliverable analyzer operation failed in PNC", updatedRequest.getReason());
        assertEquals(GenerationResult.ERR_GENERAL, updatedRequest.getResult());
    }

    private Message<String> preparePNCBuildMsg() throws IOException {
        JsonObject headers = new JsonObject();

        headers.put("type", "BuildStateChange");
        headers.put("attribute", "state-change");
        headers.put("name", "org.kie-kie-jpmml-integration-7.67.0.Final-7.13.3");
        headers.put("configurationId", "10884");
        headers.put("configurationRevision", "2653483");
        headers.put("oldStatus", "BUILDING");
        headers.put("newStatus", "SUCCESS");
        headers.put("JMSXUserID", "projectnewcastle");
        headers.put("amq6100_originalDestination", "topic://VirtualTopic.eng.pnc.builds");
        headers.put("correlationId", "a420416c-d184-4ced-9277-500667305139");
        headers.put("destination", "/topic/VirtualTopic.eng.pnc.builds");
        headers.put("messageId", "ID:orch-70-z4tx6-43917-1678809685060-25:1:8557:1:1");
        headers.put("persistent", "true");
        headers.put("producer", "PNC");

        return AmqpMessageHelper.toMessage(TestResources.asString("payloads/umb-pnc-build-body.json"), headers);
    }

    private Message<String> preparePNCDelAnalysisMsg() throws IOException {
        JsonObject headers = new JsonObject();
        headers.put("type", "DeliverableAnalysisStateChange");
        headers.put("attribute", "deliverable-analysis-state-change");
        headers.put("name", "org.kie-kie-jpmml-integration-7.67.0.Final-7.13.3");
        headers.put("milestoneId", "2712");
        headers.put("operationId", "A6DFVW2SACIAA");
        headers.put("status", "FINISHED");
        headers.put("JMSXUserID", "projectnewcastle");
        headers.put("amq6100_originalDestination", "topic://VirtualTopic.eng.pnc.builds");
        headers.put("correlationId", "a420416c-d184-4ced-9277-500667305139");
        headers.put("destination", "/topic/VirtualTopic.eng.pnc.builds");
        headers.put("messageId", "ID:orch-70-z4tx6-43917-1678809685060-25:1:8557:1:1");
        headers.put("persistent", "true");
        headers.put("producer", "PNC");

        return AmqpMessageHelper.toMessage(TestResources.asString("payloads/umb-pnc-del-analysis-body.json"), headers);
    }

    private Message<String> prepareFailedPNCDelAnalysisMsg() throws IOException {
        JsonObject headers = new JsonObject();
        headers.put("type", "DeliverableAnalysisStateChange");
        headers.put("attribute", "deliverable-analysis-state-change");
        headers.put("name", "org.kie-kie-jpmml-integration-7.67.0.Final-7.13.3");
        headers.put("milestoneId", "2712");
        headers.put("operationId", "A6DFVW2SACIAA");
        headers.put("status", "FINISHED");
        headers.put("JMSXUserID", "projectnewcastle");
        headers.put("amq6100_originalDestination", "topic://VirtualTopic.eng.pnc.builds");
        headers.put("correlationId", "a420416c-d184-4ced-9277-500667305139");
        headers.put("destination", "/topic/VirtualTopic.eng.pnc.builds");
        headers.put("messageId", "ID:orch-70-z4tx6-43917-1678809685060-25:1:8557:1:1");
        headers.put("persistent", "true");
        headers.put("producer", "PNC");

        return AmqpMessageHelper
                .toMessage(TestResources.asString("payloads/umb-pnc-failed-del-analysis-body.json"), headers);
    }

}