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
package org.jboss.sbomer.service.feature.sbom.features.umb.producer;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.jboss.sbomer.core.features.sbom.utils.ObjectMapperProvider;
import org.jboss.sbomer.service.feature.sbom.features.umb.producer.model.GenerationFinishedMessageBody;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.quarkus.arc.Unremovable;
import io.smallrye.reactive.messaging.amqp.OutgoingAmqpMetadata;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

/**
 * A message producer sending messages to the AMQP channels.
 *
 * @author Marek Goldmann
 */
@ApplicationScoped
@Unremovable
@Slf4j
public class AmqpMessageProducer {

    @Inject
    @Channel("finished")
    Instance<Emitter<String>> emitter;

    /**
     * Total number of produced messages that were NACKed by the broker.
     */
    private AtomicInteger nackedMessages = new AtomicInteger(0);

    /**
     * Total number of produced messages that were ACKed by the broker.
     */
    private AtomicInteger ackedMessages = new AtomicInteger(0);

    /**
     * Publish the provided {@link GenerationFinishedMessageBody} {@code msg} to the channel by using an {@link Emitter}
     * and wait for the acknowledgement.
     *
     * @param msg the {@link GenerationFinishedMessageBody} message body to send
     */
    public void notify(GenerationFinishedMessageBody msg) {
        if (emitter.isUnsatisfied()) {
            log.error("About to send a generation finished notification, but could not obtain the emitter");
            return;
        }

        if (msg == null) {
            log.warn("A message body was expected, but got null, not sending anything");
            return;
        }

        String data = null;

        try {
            data = ObjectMapperProvider.json().writeValueAsString(msg);
            log.debug(data);
        } catch (JsonProcessingException e) {
            // TODO: This is a fatal failure, other systems may depend on it, handle it better!
            log.error("Unable to convert message content into JSON, this is unexpected", e);
            return;
        }

        log.info(
                "Sending notification for finished SBOM generation (Generation request: '{}', SBOM id: '{}', PNC build id: '{}') using the AMQP producer",
                msg.getSbom().getGenerationRequest().getId(),
                msg.getSbom().getId(),
                msg.getBuild().getId());

        emitter.get().send(Message.of(data, () -> {
            ackedMessages.incrementAndGet();
            log.debug("Notification for SBOM id '{}' was ACKed", msg.getSbom().getId());

            return CompletableFuture.completedFuture(null);
        }, reason -> {
            log.error("Notification for SBOM id '{}' was NACKed", msg.getSbom().getId());
            log.error("Got NACK", reason);

            nackedMessages.incrementAndGet();
            return CompletableFuture.completedFuture(null);
        })
                .addMetadata(
                        OutgoingAmqpMetadata.builder()
                                .withApplicationProperty(
                                        "generation_request_id",
                                        msg.getSbom().getGenerationRequest().getId())
                                .withApplicationProperty("pnc_build_id", msg.getBuild().getId())
                                .build()));
    }

    public int getAckedMessages() {
        return ackedMessages.get();
    }

    public int getNackedMessages() {
        return nackedMessages.get();
    }
}
