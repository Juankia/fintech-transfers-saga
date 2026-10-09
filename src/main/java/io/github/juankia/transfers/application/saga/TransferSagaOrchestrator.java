package io.github.juankia.transfers.application.saga;

import io.github.juankia.transfers.application.exception.AccountNotFoundException;
import io.github.juankia.transfers.application.exception.TransferNotFoundException;
import io.github.juankia.transfers.application.port.out.AccountRepository;
import io.github.juankia.transfers.application.port.out.OutboxPort;
import io.github.juankia.transfers.application.port.out.RiskScoringPort;
import io.github.juankia.transfers.application.port.out.TransactionRunner;
import io.github.juankia.transfers.application.port.out.TransferRepository;
import io.github.juankia.transfers.domain.event.TransferEvent;
import io.github.juankia.transfers.domain.exception.DomainException;
import io.github.juankia.transfers.domain.exception.InsufficientFundsException;
import io.github.juankia.transfers.domain.model.Account;
import io.github.juankia.transfers.domain.model.FailureReason;
import io.github.juankia.transfers.domain.model.RiskAssessment;
import io.github.juankia.transfers.domain.model.Transfer;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Orchestrated saga for a money transfer:
 *
 * <pre>
 *   INITIATED --reserve--> FUNDS_RESERVED --risk--> RISK_APPROVED --debit--> DEBITED --credit--> COMPLETED
 *       |                       |                        |                      |
 *       v                       +-----------+------------+----------------------+
 *   REJECTED                                v
 *                                     COMPENSATING --release / refund--> REJECTED | FAILED
 * </pre>
 *
 * Every step is its own local ACID transaction that (a) mutates the aggregates, (b) advances the
 * transfer's status and (c) appends the resulting events to the outbox. Because the persisted
 * status always names the next step, {@link #run(UUID)} is safe to call again after a crash: it
 * resumes exactly where the previous attempt stopped. Optimistic locking on both aggregates
 * guarantees that two concurrent runners cannot both apply the same step.
 */
public class TransferSagaOrchestrator {

    private static final Logger log = Logger.getLogger(TransferSagaOrchestrator.class.getName());

    private final TransferRepository transfers;
    private final AccountRepository accounts;
    private final RiskScoringPort riskScoring;
    private final OutboxPort outbox;
    private final TransactionRunner tx;
    private final Clock clock;

    public TransferSagaOrchestrator(TransferRepository transfers, AccountRepository accounts,
                                    RiskScoringPort riskScoring, OutboxPort outbox,
                                    TransactionRunner tx, Clock clock) {
        this.transfers = transfers;
        this.accounts = accounts;
        this.riskScoring = riskScoring;
        this.outbox = outbox;
        this.tx = tx;
        this.clock = clock;
    }

    /** Drives the transfer until it reaches a terminal status and returns its final state. */
    public Transfer run(UUID transferId) {
        Transfer transfer = tx.inTransaction(() -> loadTransfer(transferId));
        while (!transfer.status().isTerminal()) {
            transfer = switch (transfer.status()) {
                case INITIATED -> reserveFunds(transferId);
                case FUNDS_RESERVED -> assessRisk(transferId);
                case RISK_APPROVED -> debitSource(transferId);
                case DEBITED -> creditBeneficiary(transferId);
                case COMPENSATING -> compensate(transferId);
                default -> throw new IllegalStateException("Unexpected status " + transfer.status());
            };
        }
        return transfer;
    }

    // ---- Step 1: hold funds on the payer account ------------------------------------------

    private Transfer reserveFunds(UUID transferId) {
        try {
            return step(transferId, transfer -> {
                Account source = loadAccount(transfer.sourceAccountId());
                source.reserve(transfer.amount());
                accounts.save(source);
                transfer.markFundsReserved(now());
            });
        } catch (DomainException e) {
            FailureReason reason = e instanceof InsufficientFundsException
                    ? FailureReason.INSUFFICIENT_FUNDS
                    : FailureReason.SOURCE_ACCOUNT_UNAVAILABLE;
            // Nothing was held, so there is nothing to compensate.
            return step(transferId, transfer -> transfer.reject(reason, e.getMessage(), now()));
        }
    }

    // ---- Step 2: fraud / risk scoring (outside any DB transaction: it may be a remote call) -

    private Transfer assessRisk(UUID transferId) {
        Transfer snapshot = tx.inTransaction(() -> loadTransfer(transferId));
        RiskAssessment assessment;
        try {
            assessment = riskScoring.assess(snapshot);
        } catch (RuntimeException e) {
            log.log(Level.WARNING, "Risk scoring unavailable for transfer " + transferId + ", failing closed", e);
            return step(transferId, transfer -> transfer.beginCompensation(
                    FailureReason.RISK_ASSESSMENT_UNAVAILABLE, e.getMessage(), now()));
        }
        return step(transferId, transfer -> {
            if (assessment.approved()) {
                transfer.markRiskApproved(assessment.score(), now());
            } else {
                transfer.recordRiskScore(assessment.score());
                transfer.beginCompensation(FailureReason.RISK_REJECTED,
                        "Risk score %d: %s".formatted(assessment.score(), String.join(", ", assessment.signals())),
                        now());
            }
        });
    }

    // ---- Step 3: consume the hold ----------------------------------------------------------

    private Transfer debitSource(UUID transferId) {
        try {
            return step(transferId, transfer -> {
                Account source = loadAccount(transfer.sourceAccountId());
                source.debitReserved(transfer.amount());
                accounts.save(source);
                transfer.markDebited(now());
            });
        } catch (DomainException e) {
            return step(transferId, transfer ->
                    transfer.beginCompensation(FailureReason.DEBIT_FAILED, e.getMessage(), now()));
        }
    }

    // ---- Step 4: credit the beneficiary and complete ---------------------------------------

    private Transfer creditBeneficiary(UUID transferId) {
        try {
            return step(transferId, transfer -> {
                Account target = loadAccount(transfer.targetAccountId());
                target.credit(transfer.amount());
                accounts.save(target);
                transfer.complete(now());
            });
        } catch (DomainException e) {
            return step(transferId, transfer ->
                    transfer.beginCompensation(FailureReason.CREDIT_FAILED, e.getMessage(), now()));
        }
    }

    // ---- Compensation: undo whatever money movement already happened ------------------------

    private Transfer compensate(UUID transferId) {
        return step(transferId, transfer -> {
            Account source = loadAccount(transfer.sourceAccountId());
            switch (transfer.compensationAction()) {
                case RELEASE_RESERVATION -> source.releaseReservation(transfer.amount());
                case REFUND_DEBIT -> source.refund(transfer.amount());
            }
            accounts.save(source);
            transfer.finishCompensation(now());
        });
    }

    /**
     * Runs one saga step in its own transaction: reload, mutate, persist, append outbox events.
     */
    private Transfer step(UUID transferId, Consumer<Transfer> action) {
        return tx.inTransaction(() -> {
            Transfer transfer = loadTransfer(transferId);
            action.accept(transfer);
            List<TransferEvent> events = transfer.pullEvents();
            Transfer saved = transfers.save(transfer);
            events.forEach(outbox::append);
            return saved;
        });
    }

    private Transfer loadTransfer(UUID id) {
        return transfers.findById(id).orElseThrow(() -> new TransferNotFoundException(id));
    }

    private Account loadAccount(UUID id) {
        return accounts.findById(id).orElseThrow(() -> new AccountNotFoundException(id));
    }

    private Instant now() {
        return clock.instant();
    }
}
