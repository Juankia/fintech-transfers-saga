package io.github.juankia.transfers.application.port.out;

import java.time.Instant;
import java.util.UUID;

public record IdempotencyRecord(String ownerId, String key, String requestHash, UUID transferId, Instant createdAt) {
}
