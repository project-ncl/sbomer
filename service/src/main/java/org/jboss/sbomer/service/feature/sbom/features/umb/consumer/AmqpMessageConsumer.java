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

import static org.jboss.sbomer.service.feature.sbom.model.UMBMessage.countErrataProcessedMessages;
import static org.jboss.sbomer.service.feature.sbom.model.UMBMessage.countErrataReceivedMessages;
import static org.jboss.sbomer.service.feature.sbom.model.UMBMessage.countPncProcessedMessages;
import static org.jboss.sbomer.service.feature.sbom.model.UMBMessage.countPncReceivedMessages;
import static org.jboss.sbomer.service.feature.sbom.model.UMBMessage.createNew;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.jboss.sbomer.core.features.sbom.enums.GenerationRequestType;
import org.jboss.sbomer.core.features.sbom.enums.UMBConsumer;
import org.jboss.sbomer.core.features.sbom.enums.UMBMessageType;
import org.jboss.sbomer.core.features.sbom.utils.ObjectMapperProvider;
import org.jboss.sbomer.service.feature.sbom.config.features.UmbConfig;
import org.jboss.sbomer.service.feature.sbom.errata.ErrataMessageHelper;
import org.jboss.sbomer.service.feature.sbom.model.UMBMessage;

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

    public void init(@Observes StartupEvent ev) {
        if (!umbConfig.isEnabled()) {
            log.info("UMB support is disabled");
            return;
        }

        log.info("Will use the reactive AMQP message consumer");
    }

    @Incoming("errata")
    @Blocking(ordered = false, value = "errata-processor-pool")
    @Transactional
    public CompletionStage<Void> processErrata(Message<byte[]> message) {
        log.debug("Received new Errata tool status change notification via the AMQP consumer");

        // Decode the message bytes to a String
        String decodedMessage = ErrataMessageHelper.decode(message.getPayload());
        log.debug("Decoded Message content: {}", decodedMessage);

        UMBMessage umbMessage = createNew(UMBConsumer.ERRATA);

        try {
            umbMessage.setContent(ObjectMapperProvider.json().readTree(decodedMessage));
        } catch (JsonProcessingException e) {
            log.warn("Could not parse into json the message payload, will not be persisted in the UMBMessage table");
        }

        umbMessage.persistAndFlush();

        // Checking whether there is some additional metadata attached to the message
        Optional<IncomingAmqpMetadata> metadata = message.getMetadata(IncomingAmqpMetadata.class);
        metadata.ifPresent(meta -> {
            JsonObject properties = meta.getProperties();
            log.debug("Message properties: {}", properties.toString());

            umbMessage.setCreationTime(Instant.ofEpochMilli(properties.getLong("timestamp")));
            umbMessage.setMsgId(properties.getString("message-id"));
            umbMessage.setTopic(properties.getString("destination"));

            if (!Objects.equals(properties.getString("subject"), "errata.activity.status")) {
                // This should not happen because we listen to the "errata.activity.status" topic, but just in case
                log.warn("Received a message that is not errata.activity.status, ignoring it");
                umbMessage.setType(UMBMessageType.UNKNOWN);
                umbMessage.ackAndSave();

                message.ack();
                return;
            }

            umbMessage.setType(UMBMessageType.ERRATA);
        });

        try {
            errataNotificationHandler.handle(decodedMessage);
        } catch (JsonProcessingException e) {
            log.error("Unable to deserialize Errata message, this is unexpected", e);
            umbMessage.nackAndSave();
            return message.nack(e);
        }

        umbMessage.ackAndSave();
        return message.ack();
    }

    @Incoming("builds")
    @Blocking(ordered = false, value = "build-processor-pool")
    @Transactional
    public CompletionStage<Void> process(Message<String> message) {
        log.debug("Received new PNC build status notification via the AMQP consumer");
        log.debug("Message content: {}", message.getPayload());

        UMBMessage umbMessage = createNew(UMBConsumer.PNC);

        try {
            umbMessage.setContent(ObjectMapperProvider.json().readTree(message.getPayload()));
        } catch (JsonProcessingException e) {
            log.warn("Could not parse into json the message payload, will not be persisted in the UMBMessage table");
        }

        umbMessage.persistAndFlush();

        // Checking whether there is some additional metadata attached to the message
        Optional<IncomingAmqpMetadata> metadata = message.getMetadata(IncomingAmqpMetadata.class);
        AtomicReference<GenerationRequestType> type = new AtomicReference<>(null);

        metadata.ifPresent(meta -> {
            JsonObject properties = meta.getProperties();
            log.debug("Message properties: {}", properties.toString());

            umbMessage.setCreationTime(Instant.ofEpochMilli(properties.getLong("timestamp")));
            umbMessage.setMsgId(properties.getString("message-id"));
            umbMessage.setTopic(properties.getString("destination"));

            if (Objects.equals(properties.getString("type"), "BuildStateChange")) {
                type.set(GenerationRequestType.BUILD);
                umbMessage.setType(UMBMessageType.BUILD);
            } else if (Objects.equals(properties.getString("type"), "DeliverableAnalysisStateChange")) {
                type.set(GenerationRequestType.OPERATION);
                umbMessage.setType(UMBMessageType.DELIVERABLE_ANALYSIS);
            } else {
                umbMessage.setType(UMBMessageType.UNKNOWN);
            }
        });

        // This shouldn't happen anymore because we use a selector to filter messages
        if (type.get() == null) {
            log.warn("Received a message that is not BuildStateChange nor DeliverableAnalysisStateChange, ignoring it");
            // I still want to persist the additional metadata if present
            if (metadata.isPresent()) {
                umbMessage.ackAndSave();
            }
            return message.ack();
        }

        try {
            pncNotificationHandler.handle(message, type.get());
        } catch (JsonProcessingException e) {
            log.error("Unable to deserialize PNC message, this is unexpected", e);
            umbMessage.nackAndSave();
            return message.nack(e);
        }

        umbMessage.ackAndSave();
        return message.ack();
    }

    public long getPncProcessedMessages() {
        return countPncProcessedMessages();
    }

    public long getPncReceivedMessages() {
        return countPncReceivedMessages();
    }

    public long getErrataProcessedMessages() {
        return countErrataProcessedMessages();
    }

    public long getErrataReceivedMessages() {
        return countErrataReceivedMessages();
    }
}
