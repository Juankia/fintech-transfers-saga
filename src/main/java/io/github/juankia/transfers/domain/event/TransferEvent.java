package io.github.juankia.transfers.domain.event;

import io.github.juankia.transfers.domain.model.FailureReason;
import io.github.juankia.transfers.domain.model.Money;

import java.time.Instant;
import java.util.UUID;

/**
 * Domain events raised by the {@code Transfer} aggregate. They are persisted to the outbox in the
 * same transaction as the state change that produced them.
 */
public sealed interface TransferEvent {

    UUID transferId();

    Instant occurredAt();

    default String type() {
        return getClass().getSimpleName();
    }

    record TransferInitiated(UUID transferId, UUID sourceAccountId, UUID targetAccountId, Money amount,
                             Instant occurredAt) implements TransferEvent { }

    record FundsReserved(UUID transferId, UUID accountId, Money amount, Instant occurredAt)
            implements TransferEvent { }

    record RiskApproved(UUID transferId, int score, Instant occurredAt) implements TransferEvent { }

    record FundsDebited(UUID transferId, UUID accountId, Money amount, Instant occurredAt)
            implements TransferEvent { }

    record FundsCredited(UUID transferId, UUID accountId, Money amount, Instant occurredAt)
            implements TransferEvent { }

    record TransferCompleted(UUID transferId, Instant occurredAt) implements TransferEvent { }

    record CompensationStarted(UUID transferId, FailureReason reason, String detail, Instant occurredAt)
            implements TransferEvent { }

    record ReservationReleased(UUID transferId, UUID accountId, Money amount, Instant occurredAt)
            implements TransferEvent { }

    record DebitReversed(UUID transferId, UUID accountId, Money amount, Instant occurredAt)
            implements TransferEvent { }

    record TransferRejected(UUID transferId, FailureReason reason, String detail, Instant occurredAt)
            implements TransferEvent { }

    record TransferFailed(UUID transferId, FailureReason reason, String detail, Instant occurredAt)
            implements TransferEvent { }
}
