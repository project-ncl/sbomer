package org.jboss.sbomer.generation.orchestrator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.BytesDeserializer;
import org.apache.kafka.common.utils.Bytes;
import org.jboss.logging.Logger;

import io.quarkus.runtime.StartupEvent;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

@ApplicationScoped
public class KafkaStats {
    private static final Logger log = Logger.getLogger(KafkaStats.class);

    void init(@Observes StartupEvent ev) {
        log.info("Initialization of Kafka stats reader...");
    }

    /**
     * A method to periodically query Kafka to get some stats.
     */
    @Scheduled(every = "5s")
    void stats() {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, BytesDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, BytesDeserializer.class);

        Stream.of("generation-request", "generation-status").forEach(channel -> {
            try (KafkaConsumer<Bytes, Bytes> consumer = new KafkaConsumer<Bytes, Bytes>(config)) {
                List<TopicPartition> partitions = consumer.partitionsFor(channel)
                        .stream()
                        .map(info -> new TopicPartition(channel, info.partition()))
                        .toList();

                consumer.assign(partitions);
                consumer.seekToEnd(partitions);

                for (TopicPartition topicPartition : partitions) {
                    log.infof("Channel '%s' messages: %s", channel, consumer.position(topicPartition));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

    }
}