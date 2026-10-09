package io.github.juankia.transfers.application.projection;

import io.github.juankia.transfers.domain.model.Money;

import java.time.Instant;
import java.util.UUID;

/**
 * Integration event consumed by the statement projection (decoupled from the wire format).
 */
public record StatementEvent(UUID eventId, String eventType, UUID transferId, UUID accountId,
                             Money amount, Instant occurredAt) {
}
