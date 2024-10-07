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

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.jboss.sbomer.core.features.sbom.enums.GenerationRequestType;
import org.jboss.sbomer.service.feature.sbom.config.features.UmbConfig;
import org.jboss.sbomer.service.feature.sbom.errata.ErrataMessageHelper;
import org.jboss.sbomer.service.feature.sbom.features.umb.consumer.model.ErrataStatusChangeMessageBody;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.quarkus.arc.Unremovable;
import io.quarkus.runtime.StartupEvent;
import io.smallrye.reactive.messaging.amqp.IncomingAmqpMetadata;
import io.smallrye.reactive.messaging.annotations.Blocking;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
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
    PncNotificationHandler pncNotificationHandler;

    @Inject
    ErrataNotificationHandler errataNotificationHandler;

    private AtomicInteger pncReceivedMessages = new AtomicInteger(0);
    private AtomicInteger pncProcessedMessages = new AtomicInteger(0);
    private AtomicInteger errataReceivedMessages = new AtomicInteger(0);
    private AtomicInteger errataProcessedMessages = new AtomicInteger(0);

    public void init(@Observes StartupEvent ev) {
        if (!umbConfig.isEnabled()) {
            log.info("UMB support is disabled");
            return;
        }

        log.info("Will use the reactive AMQP message consumer");
    }

    @Incoming("errata")
    @Blocking(ordered = false, value = "errata-processor-pool")
    public CompletionStage<Void> processErrata(Message<byte[]> message) {
        errataReceivedMessages.incrementAndGet();

        log.debug("Received new Errata tool status change notification via the AMQP consumer");

        // Decode the message bytes to a String
        String decodedMessage = ErrataMessageHelper.decode(message.getPayload());
        log.debug("Decoded Message content: {}", decodedMessage);

        // Checking whether there is some additional metadata attached to the message
        Optional<IncomingAmqpMetadata> metadata = message.getMetadata(IncomingAmqpMetadata.class);
        metadata.ifPresent(meta -> {
            JsonObject properties = meta.getProperties();

            log.debug("Message properties: {}", properties.toString());

            if (!Objects.equals(properties.getString("subject"), "errata.activity.status")) {
                // This should not happen because we listen to the "errata.activity.status" topic, but just in case
                log.warn("Received a message that is not errata.activity.status, ignoring it");
                message.ack();
                return;
            }

            // // commented for now to get more messages while we integrate with Errata
            // if (!Objects.equals(properties.getString("errata_status"), "SHIPPED_LIVE")) {
            // log.warn("Received a status change that is not SHIPPED_LIVE, ignoring it");
            // message.ack();
            // return;
            // }
        });

        try {
            errataNotificationHandler.handle(decodedMessage);
        } catch (JsonProcessingException e) {
            log.error("Unable to deserialize Errata message, this is unexpected", e);
            return message.nack(e);
        }

        errataProcessedMessages.getAndIncrement();
        return message.ack();
    }

    @Incoming("builds")
    @Blocking(ordered = false, value = "build-processor-pool")
    @Transactional
    public CompletionStage<Void> process(Message<String> message) {
        pncReceivedMessages.incrementAndGet();

        log.debug("Received new PNC build status notification via the AMQP consumer");
        log.debug("Message content: {}", message.getPayload());

        // Checking whether there is some additional metadata attached to the message
        Optional<IncomingAmqpMetadata> metadata = message.getMetadata(IncomingAmqpMetadata.class);
        AtomicReference<GenerationRequestType> type = new AtomicReference<>(null);

        metadata.ifPresent(meta -> {
            JsonObject properties = meta.getProperties();

            log.debug("Message properties: {}", properties.toString());

            if (Objects.equals(properties.getString("type"), "BuildStateChange")) {
                type.set(GenerationRequestType.BUILD);
            } else if (Objects.equals(properties.getString("type"), "DeliverableAnalysisStateChange")) {
                type.set(GenerationRequestType.OPERATION);
            }
        });

        // This shouldn't happen anymore because we use a selector to filter messages
        if (type.get() == null) {
            log.warn("Received a message that is not BuildStateChange nor DeliverableAnalysisStateChange, ignoring it");
            return message.ack();
        }

        try {
            pncNotificationHandler.handle(message, type.get());
        } catch (JsonProcessingException e) {
            log.error("Unable to deserialize PNC message, this is unexpected", e);
            return message.nack(e);
        }

        pncProcessedMessages.getAndIncrement();
        return message.ack();
    }

    public int getPncProcessedMessages() {
        return pncProcessedMessages.get();
    }

    public int getPncReceivedMessages() {
        return pncReceivedMessages.get();
    }

    public int getErrataProcessedMessages() {
        return errataProcessedMessages.get();
    }

    public int getErrataReceivedMessages() {
        return errataReceivedMessages.get();
    }
}
