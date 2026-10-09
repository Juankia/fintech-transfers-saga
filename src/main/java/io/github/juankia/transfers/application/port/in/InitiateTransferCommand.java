package io.github.juankia.transfers.application.port.in;

import io.github.juankia.transfers.domain.model.Money;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

public record InitiateTransferCommand(String requesterId, String idempotencyKey, UUID sourceAccountId,
                                      UUID targetAccountId, Money amount, String reference) {

    /**
     * Stable fingerprint of the business payload, used to detect Idempotency-Key reuse with a
     * different request. Money is already scale-normalised, so "10" and "10.00" hash the same.
     */
    public String fingerprint() {
        String canonical = String.join("|", sourceAccountId.toString(), targetAccountId.toString(),
                amount.amount().toPlainString(), amount.currency().getCurrencyCode(),
                reference == null ? "" : reference);
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(canonical.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
