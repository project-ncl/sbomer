package org.jboss.sbomer.generation.orchestrator;

import java.util.Map;
import java.util.concurrent.CompletionStage;

import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.Metadata;
import org.jboss.sbomer.core.errors.ApplicationException;
import org.jboss.sbomer.generation.status.KafkaGenerationStatusProducer;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.tekton.client.TektonClient;
import io.fabric8.tekton.pipeline.v1.ParamBuilder;
import io.fabric8.tekton.pipeline.v1.ParamValue;
import io.fabric8.tekton.pipeline.v1.TaskRefBuilder;
import io.fabric8.tekton.pipeline.v1.TaskRun;
import io.fabric8.tekton.pipeline.v1.TaskRunBuilder;
import io.quarkus.runtime.StartupEvent;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.reactive.messaging.kafka.IncomingKafkaCloudEventMetadata;
import io.smallrye.reactive.messaging.kafka.KafkaClientService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

/**
 * An example of a handled of {@link GenerationRequestZipV1Alpha1Event} that utilizes Kafka topic as the source.
 */
@ApplicationScoped
@Slf4j
public class KafkaGenerationRequestHandler {

    private static final String CHANNEL = "request";

    @Inject
    KafkaGenerationStatusProducer producer;

    @Inject
    KafkaClientService kafka;

    @Inject
    KubernetesClient kubernetesClient;

    @Inject
    TektonClient tektonClient;

    // TODO: This is a poor-man's hardcoded configuration. This should be externalized. A ConfigMap?
    private Map<String, String> eventMapping = Map
            .of("org.jboss.sbomer.generation.request.image.v1alpha1", "generator-syft");

    void init(@Observes StartupEvent ev) {
        log.info("Initialization of generation request handler...");
    }

    @Incoming(CHANNEL)
    @Retry(delay = 2, maxRetries = 5)
    @Blocking
    public CompletionStage<Void> consume(Message<String> event) {
        log.info("Got event!");

        Metadata metadata = event.getMetadata();

        if (metadata == null) {
            log.debug("Received message does not contain metadata, this is not expected");
            return event.ack();
        }

        log.info("We got metadata: {}", metadata);

        @SuppressWarnings("unchecked")
        IncomingKafkaCloudEventMetadata<String, String> recordMetadata = event
                .getMetadata(IncomingKafkaCloudEventMetadata.class)
                .orElse(null);

        if (recordMetadata == null) {
            log.debug("Received message is not a properly structured Cloud Event");
            return event.ack(); // TODO: correct failure handling
        }

        log.info(
                "Event metadata: id: {}, type: {}, topic: {}",
                recordMetadata.getId(),
                recordMetadata.getType(),
                recordMetadata.getTopic()); // This is null for now

        String data = event.getPayload();

        if (data == null) {
            log.warn("Got event, but no data was provided!");
            return event.nack(new ApplicationException("Empty event")); // TODO: correct failure handling
        }

        log.info("Event data: {}", data);

        scheduleTaskRun(recordMetadata.getType(), data);

        return event.ack();
    }

    private TaskRun scheduleTaskRun(String eventType, String eventData) {
        log.info("Scheduling a new TaskRun for '{}' event", eventType);

        final String taskName = eventMapping.get(eventType);

        log.info("Using '{}' Tekton Task", taskName);

        TaskRun taskRun = new TaskRunBuilder().withNewMetadata()
                .withGenerateName(String.format("%s-", taskName))
                .withLabels(
                        Map.of(
                                "app.kubernetes.io/part-of",
                                "sbomer",
                                "app.kubernetes.io/component",
                                "sbom",
                                "app.kubernetes.io/managed-by",
                                "sbomer",
                                "sbomer.jboss.org/type",
                                "generation-request"))
                .endMetadata()
                .withNewSpec()
                .withTaskRef(new TaskRefBuilder().withName(taskName).build())
                .withParams(new ParamBuilder().withName("request").withValue(new ParamValue(eventData)).build())
                .endSpec()
                .build();

        taskRun = tektonClient.v1().taskRuns().inNamespace("default").resource(taskRun).create();

        return taskRun;
    }
}
