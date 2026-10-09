package io.github.juankia.transfers.domain.model;

import io.github.juankia.transfers.domain.event.TransferEvent;
import io.github.juankia.transfers.domain.event.TransferEvent.*;
import io.github.juankia.transfers.domain.exception.InvalidTransferException;
import io.github.juankia.transfers.domain.exception.InvalidTransferStateException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Transfer aggregate: the saga's state machine. It only knows which transitions are legal and
 * which domain events each transition produces; moving money is the orchestrator's job.
 */
public final class Transfer {

    private static final Set<TransferStatus> COMPENSABLE =
            Set.of(TransferStatus.FUNDS_RESERVED, TransferStatus.RISK_APPROVED, TransferStatus.DEBITED);

    private final UUID id;
    private final String ownerId;
    private final UUID sourceAccountId;
    private final UUID targetAccountId;
    private final Money amount;
    private final String reference;
    private TransferStatus status;
    private FailureReason failureReason;
    private String failureDetail;
    private Integer riskScore;
    private TransferStatus compensateFrom;
    private final Instant createdAt;
    private Instant updatedAt;
    private final Long version;

    private final List<TransferEvent> pendingEvents = new ArrayList<>();

    private Transfer(UUID id, String ownerId, UUID sourceAccountId, UUID targetAccountId, Money amount,
                     String reference, TransferStatus status, FailureReason failureReason, String failureDetail,
                     Integer riskScore, TransferStatus compensateFrom, Instant createdAt, Instant updatedAt,
                     Long version) {
        this.id = Objects.requireNonNull(id);
        this.ownerId = Objects.requireNonNull(ownerId);
        this.sourceAccountId = Objects.requireNonNull(sourceAccountId);
        this.targetAccountId = Objects.requireNonNull(targetAccountId);
        this.amount = Objects.requireNonNull(amount);
        this.reference = reference;
        this.status = Objects.requireNonNull(status);
        this.failureReason = failureReason;
        this.failureDetail = failureDetail;
        this.riskScore = riskScore;
        this.compensateFrom = compensateFrom;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.version = version;
    }

    public static Transfer initiate(UUID id, String ownerId, UUID sourceAccountId, UUID targetAccountId,
                                    Money amount, String reference, Instant now) {
        if (sourceAccountId.equals(targetAccountId)) {
            throw new InvalidTransferException("Source and target accounts must differ");
        }
        if (!amount.isPositive()) {
            throw new InvalidTransferException("Transfer amount must be positive");
        }
        Transfer transfer = new Transfer(id, ownerId, sourceAccountId, targetAccountId, amount, reference,
                TransferStatus.INITIATED, null, null, null, null, now, now, null);
        transfer.raise(new TransferInitiated(id, sourceAccountId, targetAccountId, amount, now));
        return transfer;
    }

    public static Transfer rehydrate(UUID id, String ownerId, UUID sourceAccountId, UUID targetAccountId,
                                     Money amount, String reference, TransferStatus status,
                                     FailureReason failureReason, String failureDetail, Integer riskScore,
                                     TransferStatus compensateFrom, Instant createdAt, Instant updatedAt,
                                     Long version) {
        return new Transfer(id, ownerId, sourceAccountId, targetAccountId, amount, reference, status,
                failureReason, failureDetail, riskScore, compensateFrom, createdAt, updatedAt, version);
    }

    public void markFundsReserved(Instant now) {
        transition(TransferStatus.INITIATED, TransferStatus.FUNDS_RESERVED, "reserve funds", now);
        raise(new FundsReserved(id, sourceAccountId, amount, now));
    }

    /** Rejection before any money was touched: nothing to compensate. */
    public void reject(FailureReason reason, String detail, Instant now) {
        transition(TransferStatus.INITIATED, TransferStatus.REJECTED, "reject", now);
        this.failureReason = reason;
        this.failureDetail = detail;
        raise(new TransferRejected(id, reason, detail, now));
    }

    public void markRiskApproved(int score, Instant now) {
        transition(TransferStatus.FUNDS_RESERVED, TransferStatus.RISK_APPROVED, "approve risk", now);
        this.riskScore = score;
        raise(new RiskApproved(id, score, now));
    }

    public void recordRiskScore(int score) {
        this.riskScore = score;
    }

    public void markDebited(Instant now) {
        transition(TransferStatus.RISK_APPROVED, TransferStatus.DEBITED, "debit", now);
        raise(new FundsDebited(id, sourceAccountId, amount, now));
    }

    public void complete(Instant now) {
        transition(TransferStatus.DEBITED, TransferStatus.COMPLETED, "complete", now);
        raise(new FundsCredited(id, targetAccountId, amount, now));
        raise(new TransferCompleted(id, now));
    }

    public void beginCompensation(FailureReason reason, String detail, Instant now) {
        if (!COMPENSABLE.contains(status)) {
            throw new InvalidTransferStateException(id, status, "begin compensation");
        }
        this.compensateFrom = status;
        this.failureReason = reason;
        this.failureDetail = detail;
        this.status = TransferStatus.COMPENSATING;
        this.updatedAt = now;
        raise(new CompensationStarted(id, reason, detail, now));
    }

    /** Which compensating action undoes the work done so far. */
    public CompensationAction compensationAction() {
        if (status != TransferStatus.COMPENSATING) {
            throw new InvalidTransferStateException(id, status, "compute compensation");
        }
        return compensateFrom == TransferStatus.DEBITED
                ? CompensationAction.REFUND_DEBIT
                : CompensationAction.RELEASE_RESERVATION;
    }

    public void finishCompensation(Instant now) {
        CompensationAction action = compensationAction();
        raise(action == CompensationAction.REFUND_DEBIT
                ? new DebitReversed(id, sourceAccountId, amount, now)
                : new ReservationReleased(id, sourceAccountId, amount, now));
        this.status = failureReason.isBusinessRejection() ? TransferStatus.REJECTED : TransferStatus.FAILED;
        this.updatedAt = now;
        raise(status == TransferStatus.REJECTED
                ? new TransferRejected(id, failureReason, failureDetail, now)
                : new TransferFailed(id, failureReason, failureDetail, now));
    }

    /** Returns and clears the events raised since the aggregate was loaded. */
    public List<TransferEvent> pullEvents() {
        List<TransferEvent> events = List.copyOf(pendingEvents);
        pendingEvents.clear();
        return events;
    }

    private void transition(TransferStatus expected, TransferStatus next, String operation, Instant now) {
        if (status != expected) {
            throw new InvalidTransferStateException(id, status, operation);
        }
        this.status = next;
        this.updatedAt = now;
    }

    private void raise(TransferEvent event) {
        pendingEvents.add(event);
    }

    public UUID id() { return id; }
    public String ownerId() { return ownerId; }
    public UUID sourceAccountId() { return sourceAccountId; }
    public UUID targetAccountId() { return targetAccountId; }
    public Money amount() { return amount; }
    public String reference() { return reference; }
    public TransferStatus status() { return status; }
    public FailureReason failureReason() { return failureReason; }
    public String failureDetail() { return failureDetail; }
    public Integer riskScore() { return riskScore; }
    public TransferStatus compensateFrom() { return compensateFrom; }
    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }
    public Long version() { return version; }
}
