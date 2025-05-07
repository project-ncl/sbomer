/*
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
package org.jboss.sbomer.listener.umb.consumer;

import java.net.URI;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.sbomer.core.features.sbom.utils.ObjectMapperProvider;
import org.jboss.sbomer.listener.umb.config.UmbConfig;
import org.jboss.sbomer.listener.umb.handler.BrokerClient;
import org.jboss.sbomer.listener.umb.handler.ErrataNotificationHandler;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.cloudevents.CloudEvent;
import io.cloudevents.core.v1.CloudEventBuilder;
import io.quarkus.arc.Unremovable;
import io.quarkus.runtime.StartupEvent;
import io.smallrye.mutiny.Uni;
import io.smallrye.reactive.messaging.amqp.IncomingAmqpMetadata;
import io.smallrye.reactive.messaging.annotations.Blocking;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

/**
 * A UMB message consumer that utilizes the SmallRye Reactive messaging support with the AMQP connector.
 *
 * @author Marek Goldmann
 */
@ApplicationScoped
@Unremovable
@Slf4j
public class ErrataUmbMessageConsumer {

    @Inject
    UmbConfig umbConfig;

    @Inject
    ErrataNotificationHandler errataHandler;

    @RestClient
    BrokerClient brokerClient;

    ObjectMapper objectMapper = ObjectMapperProvider.json();

    public void init(@Observes StartupEvent ev) {
        if (!umbConfig.isEnabled()) {
            log.info("UMB support is disabled");
            return;
        }

        log.info("Using reactive AMQP message consumer");
    }

    @Incoming("errata")
    @Blocking(ordered = false, value = "errata-processor-pool")
    public CompletionStage<Void> processErrata(Message<byte[]> message) {
        log.debug("Received new Errata tool status change notification via the AMQP consumer");

        // Decode the message bytes to a String
        log.debug("Decoding the message...");
        // String decodedMessage = ErrataMessageHelper.decode(message.getPayload());
        // log.debug("Decoded Message content: {}", decodedMessage);

        // Checking whether there is some additional metadata attached to the message
        Optional<IncomingAmqpMetadata> metadata = message.getMetadata(IncomingAmqpMetadata.class);

        handle("a message").subscribe()
                .with(item -> System.out.println(item), failure -> System.err.println("failed with:" + failure));

        // Send the message to internal message bus
        // eventBus.send("umb-errata", message.toString());

        log.info("AmqpMessageConsumer finished processing and ending");

        return message.ack();
    }

    public Uni<String> handle(String message) {
        log.debug("Got an internal event: {}", message);

        CloudEvent cloudEvent = new CloudEventBuilder().withId(UUID.randomUUID().toString())
                .withData("application/json", "{\"key\": \"value\"}".getBytes())
                .withType("org.jboss.sbomer.generation.pnc.v1.request")
                .withSource(URI.create("http://this/is/the/source"))
                .build();

        Uni.createFrom()
                .completionStage(brokerClient.publish(cloudEvent))
                .onFailure()
                .retry()
                .withBackOff(Duration.ofSeconds(1), Duration.ofSeconds(10))
                .atMost(5)
                .log();

        // try {
        // Thread.sleep(1000);
        // } catch (InterruptedException e) {
        // // TODO Auto-generated catch block
        // e.printStackTrace();
        // }

        log.info("Processing finished");

        return Uni.createFrom().item(cloudEvent.getId());
    }

    // @Incoming("builds")
    // @Blocking(ordered = false, value = "build-processor-pool")
    // public CompletionStage<Void> process(Message<String> message) {
    // log.debug("Received new PNC build status notification via the AMQP consumer");
    // log.debug("Message content: {}", message.getPayload());

    // // Checking whether there is some additional metadata attached to the message
    // Optional<IncomingAmqpMetadata> metadata = message.getMetadata(IncomingAmqpMetadata.class);

    // return message.ack();
    // }

}
