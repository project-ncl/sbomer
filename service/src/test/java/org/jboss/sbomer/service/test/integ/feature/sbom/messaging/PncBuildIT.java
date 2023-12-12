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
import org.jboss.sbomer.core.test.TestResources;
import org.jboss.sbomer.service.feature.sbom.features.umb.consumer.AmqpMessageConsumer;
import org.jboss.sbomer.service.feature.sbom.features.umb.consumer.PncBuildNotificationHandler;
import org.jboss.sbomer.service.feature.sbom.features.umb.consumer.model.PncBuildNotificationMessageBody;
import org.jboss.sbomer.service.feature.sbom.features.umb.producer.AmqpMessageProducer;
import org.jboss.sbomer.service.feature.sbom.k8s.model.GenerationRequest;
import org.jboss.sbomer.service.test.utils.AmqpMessageHelper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectSpy;
import io.quarkus.test.kubernetes.client.WithKubernetesTestServer;
import io.smallrye.reactive.messaging.memory.InMemoryConnector;
import io.smallrye.reactive.messaging.memory.InMemorySource;
import io.vertx.amqp.AmqpMessage;
import io.vertx.amqp.AmqpMessageBuilder;
import io.vertx.core.json.JsonObject;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@QuarkusTest
@QuarkusTestResource(AmqpTestResourceLifecycleManager.class)
@Slf4j
@WithKubernetesTestServer
public class PncBuildIT {

    @Inject
    AmqpMessageConsumer consumer;

    @Inject
    AmqpMessageProducer producer;

    @InjectSpy
    PncBuildNotificationHandler handler;

    @Inject
    KubernetesClient kubernetesClient;

    @Inject
    @Connector("smallrye-in-memory")
    InMemoryConnector connector;

    @Test
    public void testUMBProducerPNCBuild() throws Exception {
        log.info("Running testUMBProducerPNCBuild...");

        InMemorySource<Message<String>> builds = connector.source("builds");
        Message<String> txgMsg = preparePNCBuildMsg();

        builds.send(txgMsg);

        ArgumentCaptor<PncBuildNotificationMessageBody> argumentCaptor = ArgumentCaptor
                .forClass(PncBuildNotificationMessageBody.class);

        Awaitility.await().atMost(30, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(() -> {
            List<ConfigMap> configMaps = kubernetesClient.configMaps().list().getItems();

            Optional<ConfigMap> request = configMaps.stream()
                    .filter(cm -> cm.getData().get(GenerationRequest.KEY_BUILD_ID).equals("AX5TJMYHQAIAE"))
                    .findFirst();

            if (request.isPresent()) {
                log.info("Generation request was found!");
                return true;
            }

            return false;
        });

        verify(handler, times(1)).handle(argumentCaptor.capture());
        List<PncBuildNotificationMessageBody> messages = argumentCaptor.getAllValues();

        assertEquals(1, messages.size());

        // See "payloads/umb-pnc-build-body.json" file
        assertEquals(messages.get(0).getBuild().getId(), "AX5TJMYHQAIAE");
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
}