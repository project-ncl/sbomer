package org.jboss.sbomer.generation.status;

import java.net.URI;
import java.time.temporal.ChronoUnit;

import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.jboss.logging.Logger;

import io.quarkus.runtime.StartupEvent;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.smallrye.reactive.messaging.ce.OutgoingCloudEventMetadata;
import io.smallrye.reactive.messaging.kafka.KafkaClientService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

@ApplicationScoped
@RegisterForReflection
public class KafkaGenerationStatusProducer {
    private static final Logger log = Logger.getLogger(GenerationStatusEvent.class);

    private static final String CHANNEL = "status";

    void init(@Observes StartupEvent ev) {
        log.info("Initialization of status updater...");
    }

    @Inject
    @Channel(CHANNEL)
    Emitter<GenerationStatusEvent> emitter;

    @Inject
    KafkaClientService kafka;

    @Retry(delay = 1000, delayUnit = ChronoUnit.MILLIS, maxRetries = 5)
    public void send(GenerationStatus status) {
        log.info("Sending status message...");

        OutgoingCloudEventMetadata<Object> metadata = OutgoingCloudEventMetadata.builder()
                .withType(GenerationStatusEvent.TYPE_ID)
                .withSource(URI.create("https://sbomer/generator/delan"))
                .build();

        GenerationStatusEvent statusEvent = GenerationStatusEvent.builder()
                .withSpec(GenerationStatusEvent.Spec.builder().withStatus(status).build())
                .build();

        Message<GenerationStatusEvent> message = Message.of(statusEvent);
        message.addMetadata(metadata);

        log.debugf("Message: %s", message.getPayload());

        emitter.send(message);
    }
}
