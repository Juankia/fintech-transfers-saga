package io.github.juankia.transfers.application.projection;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Row of the account-statement read model. {@code amount} is signed: negative for money out.
 */
public record StatementEntry(UUID eventId, UUID accountId, UUID transferId, EntryType type,
                             BigDecimal amount, String currency, Instant occurredAt) {

    public enum EntryType { DEBIT, CREDIT, REVERSAL }
}
