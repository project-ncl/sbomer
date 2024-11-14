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

import static org.jboss.sbomer.service.feature.sbom.model.RequestEvent.EVENT_KEY_UMB_CONSUMER;
import static org.jboss.sbomer.service.feature.sbom.model.RequestEvent.EVENT_KEY_UMB_MSG_STATUS;
import static org.jboss.sbomer.service.feature.sbom.model.RequestEvent.EVENT_KEY_UMB_MSG;
import static org.jboss.sbomer.service.feature.sbom.model.RequestEvent.EVENT_KEY_UMB_MSG_TYPE;
import static org.jboss.sbomer.service.feature.sbom.model.RequestEvent.EVENT_KEY_UMB_MSG_CREATION_TIME;
import static org.jboss.sbomer.service.feature.sbom.model.RequestEvent.EVENT_KEY_UMB_MSG_ID;
import static org.jboss.sbomer.service.feature.sbom.model.RequestEvent.EVENT_KEY_UMB_TOPIC;
import static org.jboss.sbomer.service.feature.sbom.model.RequestEvent.EVENT_VALUE_UMB_UNKNOWN_MSG_TYPE;

import java.io.IOException;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.jboss.sbomer.core.config.request.ErrataAdvisoryRequestConfig;
import org.jboss.sbomer.core.config.request.PncBuildRequestConfig;
import org.jboss.sbomer.core.config.request.PncOperationRequestConfig;
import org.jboss.sbomer.core.features.sbom.enums.UMBConsumer;
import org.jboss.sbomer.core.features.sbom.enums.UMBMessageStatus;
import org.jboss.sbomer.core.features.sbom.utils.ObjectMapperProvider;
import org.jboss.sbomer.service.feature.sbom.config.features.UmbConfig;
import org.jboss.sbomer.service.feature.sbom.errata.ErrataMessageHelper;
import org.jboss.sbomer.service.feature.sbom.model.RequestEvent;
import org.jboss.sbomer.service.feature.sbom.model.RequestEventType;
import org.jboss.sbomer.service.feature.sbom.service.RequestEventRepository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;

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

    @Inject
    RequestEventRepository repository;

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
        log.debug("Received new Errata tool status change notification via the AMQP consumer");

        // Decode the message bytes to a String
        String decodedMessage = ErrataMessageHelper.decode(message.getPayload());
        log.debug("Decoded Message content: {}", decodedMessage);

        ObjectNode event = createUnidentifiedEvent(decodedMessage, UMBConsumer.ERRATA);

        // Checking whether there is some additional metadata attached to the message
        Optional<IncomingAmqpMetadata> metadata = message.getMetadata(IncomingAmqpMetadata.class);

        if (metadata.isPresent()) {
            addMetadataToEvent(metadata.get(), event);
            identifyErrataEvent(metadata.get(), event);
        }

        if (!isIdentifiedEvent(event)) {
            return ackAndSaveUnknownMessage(message, event);
        }

        // Store the requestEvent (to keep events in case of subsequent failures)
        RequestEvent requestEvent = saveNewEvent(event);

        if (hasMessageId(event)) {
            // Verify that there aren't already ACKED UMBMessages with the same msg id
            // There is an issue in our queues and same messages are processed multiple times, we want to avoid
            // generating manifests for the same event
            String msgId = event.get(EVENT_KEY_UMB_MSG_ID).asText();
            long alreadyGenerated = repository.countAlreadyAckedUMBEventsFor(msgId);
            if (alreadyGenerated > 0) {
                log.warn(
                        "Message with id '{}' has been already received and processed {} times!! Will not process it again, skipping it",
                        msgId,
                        alreadyGenerated);

                return skipAndSave(message, requestEvent);
            }
        }

        try {
            errataNotificationHandler.handle(requestEvent);
        } catch (IOException e) {
            log.error("Unable to deserialize Errata message, this is unexpected", e);
            return nackAndSave(message, requestEvent, e);
        }

        return ackAndSave(message, requestEvent);
    }

    @Incoming("builds")
    @Blocking(ordered = false, value = "build-processor-pool")
    public CompletionStage<Void> process(Message<String> message) {
        log.debug("Received new PNC build status notification via the AMQP consumer");
        log.debug("Message content: {}", message.getPayload());

        ObjectNode event = createUnidentifiedEvent(message.getPayload(), UMBConsumer.PNC);

        // Checking whether there is some additional metadata attached to the message
        Optional<IncomingAmqpMetadata> metadata = message.getMetadata(IncomingAmqpMetadata.class);

        if (metadata.isPresent()) {
            addMetadataToEvent(metadata.get(), event);
            identifyPncEvent(metadata.get(), event);
        }

        if (!isIdentifiedEvent(event)) {
            return ackAndSaveUnknownMessage(message, event);
        }

        // Store the requestEvent (to keep events in case of subsequent failures)
        RequestEvent requestEvent = saveNewEvent(event);

        if (hasMessageId(event)) {
            // Verify that there aren't already ACKED UMBMessages with the same msg id
            // There is an issue in our queues and same messages are processed multiple times, we want to avoid
            // generating manifests for the same event
            String msgId = event.get(EVENT_KEY_UMB_MSG_ID).asText();
            long alreadyGenerated = repository.countAlreadyAckedUMBEventsFor(msgId);
            if (alreadyGenerated > 0) {
                log.warn(
                        "Message with id '{}' has been already received and processed {} times!! Will not process it again, skipping it",
                        msgId,
                        alreadyGenerated);

                return skipAndSave(message, requestEvent);
            }
        }

        try {
            pncNotificationHandler.handle(requestEvent);
        } catch (JsonProcessingException e) {
            log.error("Unable to deserialize PNC message, this is unexpected", e);
            return nackAndSave(message, requestEvent, e);
        }

        return ackAndSave(message, requestEvent);
    }

    private void identifyErrataEvent(IncomingAmqpMetadata metadata, ObjectNode event) {

        JsonObject properties = metadata.getProperties();
        log.debug("Message properties: {}", properties.toString());

        if (Objects.equals(properties.getString("subject"), "errata.activity.status")) {
            event.put(EVENT_KEY_UMB_MSG_TYPE, ErrataAdvisoryRequestConfig.TYPE_NAME);
        } else {
            log.warn("Received an Errata message that is not of subject 'errata.activity.status', ignoring it");
        }
    }

    private void identifyPncEvent(IncomingAmqpMetadata metadata, ObjectNode event) {

        JsonObject properties = metadata.getProperties();
        log.debug("Message properties: {}", properties.toString());

        if (Objects.equals(properties.getString("type"), "BuildStateChange")) {
            event.put(EVENT_KEY_UMB_MSG_TYPE, PncBuildRequestConfig.TYPE_NAME);
        } else if (Objects.equals(properties.getString("type"), "DeliverableAnalysisStateChange")) {
            event.put(EVENT_KEY_UMB_MSG_TYPE, PncOperationRequestConfig.TYPE_NAME);
        } else {
            log.warn(
                    "Received a PNC message that is not of type 'BuildStateChange' nor 'DeliverableAnalysisStateChange', ignoring it");
        }
    }

    private boolean isIdentifiedEvent(ObjectNode event) {
        return event.has(EVENT_KEY_UMB_MSG_TYPE);
    }

    private boolean hasMessageId(ObjectNode event) {
        return event.has(EVENT_KEY_UMB_MSG_ID);
    }

    @Transactional
    protected CompletionStage<Void> nackAndSave(Message<?> message, RequestEvent requestEvent, Throwable e) {
        ((ObjectNode) requestEvent.getEvent()).put(EVENT_KEY_UMB_MSG_STATUS, UMBMessageStatus.NACK.toString());
        requestEvent.save();

        return message.nack(e);
    }

    @Transactional
    protected CompletionStage<Void> ackAndSave(Message<?> message, RequestEvent requestEvent) {
        ((ObjectNode) requestEvent.getEvent()).put(EVENT_KEY_UMB_MSG_STATUS, UMBMessageStatus.ACK.toString());
        requestEvent.save();

        return message.ack();
    }

    @Transactional
    protected CompletionStage<Void> skipAndSave(Message<?> message, RequestEvent requestEvent) {
        ((ObjectNode) requestEvent.getEvent()).put(EVENT_KEY_UMB_MSG_STATUS, UMBMessageStatus.SKIPPED.toString());
        requestEvent.save();

        return message.ack();
    }

    @Transactional
    protected CompletionStage<Void> ackAndSaveUnknownMessage(Message<?> message, ObjectNode event) {
        event.put(EVENT_KEY_UMB_MSG_TYPE, EVENT_VALUE_UMB_UNKNOWN_MSG_TYPE)
                .put(EVENT_KEY_UMB_MSG_STATUS, UMBMessageStatus.ACK.toString());
        RequestEvent.createNew(null, RequestEventType.UMB, event).save();
        return message.ack();
    }

    @Transactional
    protected RequestEvent saveNewEvent(ObjectNode event) {
        return RequestEvent.createNew(null, RequestEventType.UMB, event).save();
    }

    private ObjectNode createUnidentifiedEvent(String content, UMBConsumer consumer) {
        return ObjectMapperProvider.json()
                .createObjectNode()
                .put(EVENT_KEY_UMB_CONSUMER, consumer.toString())
                .put(EVENT_KEY_UMB_MSG, content)
                .put(EVENT_KEY_UMB_MSG_STATUS, UMBMessageStatus.NONE.toString());
    }

    private ObjectNode addMetadataToEvent(IncomingAmqpMetadata metadata, ObjectNode event) {
        return event.put(EVENT_KEY_UMB_MSG_CREATION_TIME, Instant.ofEpochMilli(metadata.getCreationTime()).toString())
                .put(EVENT_KEY_UMB_MSG_ID, metadata.getId())
                .put(EVENT_KEY_UMB_TOPIC, metadata.getAddress());
    }

    public long getPncProcessedMessages() {
        return repository.countPncProcessedMessages();
    }

    public long getPncReceivedMessages() {
        return repository.countPncReceivedMessages();
    }

    public long getErrataProcessedMessages() {
        return repository.countErrataProcessedMessages();
    }

    public long getErrataReceivedMessages() {
        return repository.countErrataReceivedMessages();
    }

    public long getPncSkippedMessages() {
        return repository.countPncSkippedMessages();
    }

    public long getErrataSkippedMessages() {
        return repository.countErrataSkippedMessages();
    }

}
