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
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.jboss.sbomer.core.config.request.ErrataAdvisoryRequestConfig;
import org.jboss.sbomer.core.config.request.PncBuildRequestConfig;
import org.jboss.sbomer.core.config.request.PncOperationRequestConfig;
import org.jboss.sbomer.core.errors.ApplicationException;
import org.jboss.sbomer.core.features.sbom.enums.RequestEventStatus;
import org.jboss.sbomer.core.features.sbom.enums.UMBConsumer;
import org.jboss.sbomer.core.features.sbom.enums.UMBMessageStatus;
import org.jboss.sbomer.core.features.sbom.utils.ObjectMapperProvider;
import org.jboss.sbomer.service.feature.sbom.config.features.UmbConfig;
import org.jboss.sbomer.service.feature.sbom.errata.ErrataMessageHelper;
import org.jboss.sbomer.service.feature.sbom.model.RequestEvent;
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
import jakarta.transaction.Transactional.TxType;
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
    RequestEventRepository requestEventRepository;

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
            long alreadyGenerated = getAlreadyAckedUMBEventsFor(msgId);
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
        } catch (ApplicationException exc) {
            log.error("Received error while handing request '{}': {}", requestEvent.getId(), exc.getMessage());
            return nackAndSave(message, requestEvent, exc);
        } catch (RuntimeException exc) {
            log.error("Received error while handing request '{}'", requestEvent.getId(), exc);
            return nackAndSave(message, requestEvent, exc);
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
            long alreadyGenerated = getAlreadyAckedUMBEventsFor(msgId);
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
        } catch (ApplicationException exc) {
            log.error("Received error while handing request '{}': {}", requestEvent.getId(), exc.getMessage());
            return nackAndSave(message, requestEvent, exc);
        } catch (RuntimeException exc) {
            log.error("Received error while handing request '{}'", requestEvent.getId(), exc);
            return nackAndSave(message, requestEvent, exc);
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

    @Transactional(value = TxType.REQUIRES_NEW)
    protected CompletionStage<Void> nackAndSave(Message<?> message, RequestEvent requestEvent, Throwable e) {
        requestEventRepository.updateRequestEvent(
                requestEvent,
                RequestEventStatus.FAILED,
                Map.of(EVENT_KEY_UMB_MSG_STATUS, UMBMessageStatus.NACK.toString()),
                RequestEvent.FAILED_GENERIC_REASON);
        return message.nack(e);
    }

    @Transactional(value = TxType.REQUIRES_NEW)
    protected CompletionStage<Void> ackAndSave(Message<?> message, RequestEvent requestEvent) {
        requestEventRepository.updateRequestEvent(
                requestEvent,
                null,
                Map.of(EVENT_KEY_UMB_MSG_STATUS, UMBMessageStatus.ACK.toString()),
                null);
        return message.ack();
    }

    @Transactional(value = TxType.REQUIRES_NEW)
    protected CompletionStage<Void> skipAndSave(Message<?> message, RequestEvent requestEvent) {
        requestEventRepository.updateRequestEvent(
                requestEvent,
                RequestEventStatus.IGNORED,
                Map.of(EVENT_KEY_UMB_MSG_STATUS, UMBMessageStatus.SKIPPED.toString()),
                RequestEvent.IGNORED_DUPLICATED_REASON);
        return message.ack();
    }

    @Transactional(value = TxType.REQUIRES_NEW)
    protected CompletionStage<Void> ackAndSaveUnknownMessage(Message<?> message, ObjectNode event) {
        Map<String, String> extra = Map.of(
                EVENT_KEY_UMB_MSG_TYPE,
                EVENT_VALUE_UMB_UNKNOWN_MSG_TYPE,
                EVENT_KEY_UMB_MSG_STATUS,
                UMBMessageStatus.ACK.toString());
        extra.forEach(event::put);
        requestEventRepository
                .createRequestEvent(RequestEventStatus.IGNORED, event, RequestEvent.IGNORED_UNKNOWN_REASON);
        return message.ack();
    }

    @Transactional(value = TxType.REQUIRES_NEW)
    protected RequestEvent saveNewEvent(ObjectNode event) {
        return requestEventRepository.createRequestEvent(null, event, null);
    }

    @Transactional
    public long getAlreadyAckedUMBEventsFor(String msgId) {
        return requestEventRepository.countAlreadyAckedUMBEventsFor(msgId);
    }

    @Transactional
    public long getPncProcessedMessages() {
        return requestEventRepository.countPncProcessedMessages();
    }

    @Transactional
    public long getPncReceivedMessages() {
        return requestEventRepository.countPncReceivedMessages();
    }

    @Transactional
    public long getErrataProcessedMessages() {
        return requestEventRepository.countErrataProcessedMessages();
    }

    @Transactional
    public long getErrataReceivedMessages() {
        return requestEventRepository.countErrataReceivedMessages();
    }

    @Transactional
    public long getPncSkippedMessages() {
        return requestEventRepository.countPncSkippedMessages();
    }

    @Transactional
    public long getErrataSkippedMessages() {
        return requestEventRepository.countErrataSkippedMessages();
    }

}
