package io.github.juankia.transfers.infrastructure.web;

import io.github.juankia.transfers.domain.model.Transfer;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransferResponse(UUID id, UUID sourceAccountId, UUID targetAccountId, BigDecimal amount,
                               String currency, String reference, String status, String failureReason,
                               String failureDetail, Integer riskScore, Instant createdAt, Instant updatedAt) {

    static TransferResponse from(Transfer t) {
        return new TransferResponse(t.id(), t.sourceAccountId(), t.targetAccountId(), t.amount().amount(),
                t.amount().currency().getCurrencyCode(), t.reference(), t.status().name(),
                t.failureReason() == null ? null : t.failureReason().name(), t.failureDetail(), t.riskScore(),
                t.createdAt(), t.updatedAt());
    }
}
