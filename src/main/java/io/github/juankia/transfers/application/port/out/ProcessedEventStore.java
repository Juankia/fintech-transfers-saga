package io.github.juankia.transfers.application.port.out;

import java.util.UUID;

/**
 * Consumer-side deduplication ("inbox"). Kafka delivers at-least-once; this makes handlers
 * effectively-once.
 */
public interface ProcessedEventStore {

    /** @return {@code false} if the event had already been processed by this consumer. */
    boolean markProcessed(UUID eventId, String consumer);
}
