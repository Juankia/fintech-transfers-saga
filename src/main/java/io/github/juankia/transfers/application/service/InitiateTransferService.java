package io.github.juankia.transfers.application.service;

import io.github.juankia.transfers.application.exception.AccountAccessDeniedException;
import io.github.juankia.transfers.application.exception.AccountNotFoundException;
import io.github.juankia.transfers.application.exception.DuplicateIdempotencyKeyException;
import io.github.juankia.transfers.application.exception.IdempotencyKeyConflictException;
import io.github.juankia.transfers.application.exception.TransferNotFoundException;
import io.github.juankia.transfers.application.port.in.InitiateTransferCommand;
import io.github.juankia.transfers.application.port.in.InitiateTransferResult;
import io.github.juankia.transfers.application.port.in.InitiateTransferUseCase;
import io.github.juankia.transfers.application.port.out.AccountRepository;
import io.github.juankia.transfers.application.port.out.IdempotencyRecord;
import io.github.juankia.transfers.application.port.out.IdempotencyStore;
import io.github.juankia.transfers.application.port.out.OutboxPort;
import io.github.juankia.transfers.application.port.out.TransactionRunner;
import io.github.juankia.transfers.application.port.out.TransferRepository;
import io.github.juankia.transfers.application.saga.TransferSagaOrchestrator;
import io.github.juankia.transfers.domain.model.Account;
import io.github.juankia.transfers.domain.model.Transfer;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Entry point for POST /transfers.
 *
 * <ol>
 *   <li>Same (requester, Idempotency-Key) seen before: same payload replays the stored transfer,
 *       a different payload is a conflict.</li>
 *   <li>Otherwise the transfer and its idempotency record are inserted atomically; a unique
 *       constraint on the key resolves races between concurrent duplicates.</li>
 *   <li>The saga is then driven to a terminal status.</li>
 * </ol>
 */
public class InitiateTransferService implements InitiateTransferUseCase {

    private final AccountRepository accounts;
    private final TransferRepository transfers;
    private final IdempotencyStore idempotencyStore;
    private final OutboxPort outbox;
    private final TransferSagaOrchestrator orchestrator;
    private final TransactionRunner tx;
    private final Clock clock;
    private final Supplier<UUID> idGenerator;

    public InitiateTransferService(AccountRepository accounts, TransferRepository transfers,
                                   IdempotencyStore idempotencyStore, OutboxPort outbox,
                                   TransferSagaOrchestrator orchestrator, TransactionRunner tx,
                                   Clock clock, Supplier<UUID> idGenerator) {
        this.accounts = accounts;
        this.transfers = transfers;
        this.idempotencyStore = idempotencyStore;
        this.outbox = outbox;
        this.orchestrator = orchestrator;
        this.tx = tx;
        this.clock = clock;
        this.idGenerator = idGenerator;
    }

    @Override
    public InitiateTransferResult initiate(InitiateTransferCommand command) {
        String fingerprint = command.fingerprint();

        var previous = idempotencyStore.find(command.requesterId(), command.idempotencyKey());
        if (previous.isPresent()) {
            return replay(previous.get(), command, fingerprint);
        }

        authorize(command);

        Transfer created;
        try {
            created = tx.inTransaction(() -> createTransfer(command, fingerprint));
        } catch (DuplicateIdempotencyKeyException raceLost) {
            IdempotencyRecord winner = idempotencyStore.find(command.requesterId(), command.idempotencyKey())
                    .orElseThrow(() -> raceLost);
            return replay(winner, command, fingerprint);
        }

        return new InitiateTransferResult(orchestrator.run(created.id()), false);
    }

    private Transfer createTransfer(InitiateTransferCommand command, String fingerprint) {
        Instant now = clock.instant();
        Transfer transfer = Transfer.initiate(idGenerator.get(), command.requesterId(), command.sourceAccountId(),
                command.targetAccountId(), command.amount(), command.reference(), now);
        var events = transfer.pullEvents();
        Transfer saved = transfers.save(transfer);
        idempotencyStore.register(new IdempotencyRecord(command.requesterId(), command.idempotencyKey(),
                fingerprint, saved.id(), now));
        events.forEach(outbox::append);
        return saved;
    }

    private void authorize(InitiateTransferCommand command) {
        Account source = accounts.findById(command.sourceAccountId())
                .orElseThrow(() -> new AccountNotFoundException(command.sourceAccountId()));
        if (!source.isOwnedBy(command.requesterId())) {
            throw new AccountAccessDeniedException(command.sourceAccountId());
        }
        accounts.findById(command.targetAccountId())
                .orElseThrow(() -> new AccountNotFoundException(command.targetAccountId()));
    }

    private InitiateTransferResult replay(IdempotencyRecord record, InitiateTransferCommand command, String fingerprint) {
        if (!record.requestHash().equals(fingerprint)) {
            throw new IdempotencyKeyConflictException(command.idempotencyKey());
        }
        Transfer transfer = transfers.findById(record.transferId())
                .orElseThrow(() -> new TransferNotFoundException(record.transferId()));
        return new InitiateTransferResult(transfer, true);
    }
}
