package io.github.juankia.transfers.infrastructure.messaging;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import io.github.juankia.transfers.application.port.in.ProjectStatementEventUseCase;
import io.github.juankia.transfers.application.projection.StatementEvent;
import io.github.juankia.transfers.application.projection.StatementProjection;
import io.github.juankia.transfers.domain.model.Money;
import io.github.juankia.transfers.infrastructure.outbox.OutboxRelay;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

/**
 * Kafka adapter for the statement read model. Translates the wire format into a
 * {@link StatementEvent}; deduplication happens in the application layer.
 */
@Component
class StatementEventsListener {

    private static final Logger log = LoggerFactory.getLogger(StatementEventsListener.class);

    private final ProjectStatementEventUseCase projection;
    private final ObjectReader reader;

    StatementEventsListener(ProjectStatementEventUseCase projection, ObjectMapper objectMapper) {
        this.projection = projection;
        this.reader = objectMapper.reader().with(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);
    }

    @KafkaListener(topics = "${transfers.kafka.topic}", groupId = "statement-projection")
    void onEvent(ConsumerRecord<String, String> record) throws IOException {
        String eventType = header(record, OutboxRelay.HEADER_EVENT_TYPE);
        if (eventType == null || !StatementProjection.handles(eventType)) {
            return;
        }
        UUID eventId = UUID.fromString(header(record, OutboxRelay.HEADER_EVENT_ID));
        JsonNode payload = reader.readTree(record.value());
        JsonNode amount = payload.get("amount");
        var event = new StatementEvent(eventId, eventType,
                UUID.fromString(payload.get("transferId").asText()),
                UUID.fromString(payload.get("accountId").asText()),
                Money.of(amount.get("amount").decimalValue(), amount.get("currency").asText()),
                Instant.parse(payload.get("occurredAt").asText()));
        boolean applied = projection.project(event);
        log.debug("{} {} for transfer {} {}", eventType, eventId, event.transferId(), applied ? "projected" : "skipped (duplicate)");
    }

    private static String header(ConsumerRecord<?, ?> record, String name) {
        Header header = record.headers().lastHeader(name);
        return header == null ? null : new String(header.value(), StandardCharsets.UTF_8);
    }
}
