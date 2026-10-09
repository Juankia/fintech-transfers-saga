package io.github.juankia.transfers.application.port.in;

import io.github.juankia.transfers.domain.model.Transfer;

/**
 * @param replayed {@code true} when the response comes from a previous request with the same Idempotency-Key
 */
public record InitiateTransferResult(Transfer transfer, boolean replayed) {
}
