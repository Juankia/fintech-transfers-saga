package io.github.juankia.transfers.infrastructure.outbox;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Polls the outbox and publishes pending events to Kafka (at-least-once). Records are keyed by
 * aggregate id so that all events of one transfer land on the same partition, in order. If a
 * send fails the batch stops, preserving order; the next poll retries.
 */
@Component
@ConditionalOnProperty(name = "transfers.outbox.relay-enabled", havingValue = "true", matchIfMissing = true)
public class OutboxRelay {

    public static final String HEADER_EVENT_ID = "eventId";
    public static final String HEADER_EVENT_TYPE = "eventType";
    public static final String HEADER_AGGREGATE_TYPE = "aggregateType";

    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);

    private final OutboxEventJpaRepository repository;
    private final KafkaTemplate<String, String> kafka;
    private final Clock clock;
    private final String topic;

    public OutboxRelay(OutboxEventJpaRepository repository, KafkaTemplate<String, String> kafka, Clock clock,
                       @Value("${transfers.kafka.topic}") String topic) {
        this.repository = repository;
        this.kafka = kafka;
        this.clock = clock;
        this.topic = topic;
    }

    @Scheduled(fixedDelayString = "${transfers.outbox.poll-interval-ms:500}")
    @Transactional
    public int publishPending() {
        List<OutboxEventEntity> batch = repository.findTop100ByPublishedAtIsNullOrderByIdAsc();
        int published = 0;
        for (OutboxEventEntity event : batch) {
            try {
                var record = new ProducerRecord<String, String>(topic, event.getAggregateId().toString(), event.getPayload());
                record.headers()
                        .add(HEADER_EVENT_ID, event.getEventId().toString().getBytes(StandardCharsets.UTF_8))
                        .add(HEADER_EVENT_TYPE, event.getEventType().getBytes(StandardCharsets.UTF_8))
                        .add(HEADER_AGGREGATE_TYPE, event.getAggregateType().getBytes(StandardCharsets.UTF_8));
                kafka.send(record).get(10, TimeUnit.SECONDS);
                event.markPublished(clock.instant());
                published++;
            } catch (Exception e) {
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                log.warn("Outbox publish failed for event {} ({}); will retry", event.getEventId(), event.getEventType(), e);
                break;
            }
        }
        return published;
    }
}
