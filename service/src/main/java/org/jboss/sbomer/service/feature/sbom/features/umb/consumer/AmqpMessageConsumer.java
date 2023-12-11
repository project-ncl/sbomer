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
package org.jboss.sbomer.service.feature.sbom.features.umb.consumer;

import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.jboss.sbomer.core.features.sbom.utils.ObjectMapperProvider;
import org.jboss.sbomer.service.feature.sbom.config.features.UmbConfig;
import org.jboss.sbomer.service.feature.sbom.features.umb.consumer.model.PncBuildNotificationMessageBody;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.quarkus.arc.Unremovable;
import io.quarkus.runtime.StartupEvent;
import io.smallrye.reactive.messaging.amqp.IncomingAmqpMetadata;
import io.smallrye.reactive.messaging.annotations.Blocking;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

/**
 * An UMB message consumer that utilizes the SmallRye Reactive messaging support with the AMQ connector.
 *
 * @author Marek Goldmann
 */
@ApplicationScoped
@Unremovable
@Slf4j
public class AmqpMessageConsumer {

    @Inject
    UmbConfig umbConfig;

    @Inject
    PncBuildNotificationHandler buildNotificationHandler;

    private AtomicInteger receivedMessages = new AtomicInteger(0);
    private AtomicInteger processedMessages = new AtomicInteger(0);

    public void init(@Observes StartupEvent ev) {
        if (!umbConfig.isEnabled()) {
            log.info("UMB support is disabled");
            return;
        }

        log.info("Will use the reactive AMQP message consumer");

        if (!umbConfig.isEnabled()) {
            log.warn(
                    "UMB feature is disabled, but this setting will be ignored because the 'sbomer.features.umb.reactive' is set to true");
            return;
        }

        if (!umbConfig.consumer().isEnabled()) {
            log.info(
                    "UMB feature to consume messages is disabled, but this setting will be ignored because the 'sbomer.features.umb.reactive' is set to true");
            return;
        }
    }

    @Incoming("builds")
    @Blocking(ordered = false, value = "build-processor-pool")
    public CompletionStage<Void> process(Message<String> message) {
        log.debug("Received new message via the AMQP consumer");
        log.debug("Message content: {}", message.getPayload());

        receivedMessages.incrementAndGet();

        // Checking whether there is some additional metadata attached to the message
        Optional<IncomingAmqpMetadata> metadata = message.getMetadata(IncomingAmqpMetadata.class);

        metadata.ifPresent(meta -> {
            JsonObject properties = meta.getProperties();

            log.trace(properties.toString());

            String correlationId = properties.getString("correlation-id");
            String messageId = properties.getString("message-id");
            Long timestamp = properties.getLong("timestamp");

            log.debug(
                    "Additional metadata: correlation-id: {}, message-id: {}, timestamp: {}",
                    correlationId,
                    messageId,
                    timestamp);
        });

        PncBuildNotificationMessageBody body = null;

        try {
            body = ObjectMapperProvider.json().readValue(message.getPayload(), PncBuildNotificationMessageBody.class);
        } catch (JsonProcessingException e) {
            log.error("Unable to deserialize PNC build finished message, this is unexpected", e);
            return message.nack(e);
        }

        log.debug("Message properly deserialized");

        buildNotificationHandler.handle(body);

        processedMessages.getAndIncrement();

        return message.ack();
    }
}
