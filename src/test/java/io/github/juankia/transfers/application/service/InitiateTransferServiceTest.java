package io.github.juankia.transfers.application.service;

import io.github.juankia.transfers.application.exception.AccountAccessDeniedException;
import io.github.juankia.transfers.application.exception.DuplicateIdempotencyKeyException;
import io.github.juankia.transfers.application.exception.IdempotencyKeyConflictException;
import io.github.juankia.transfers.application.port.in.InitiateTransferCommand;
import io.github.juankia.transfers.application.port.in.InitiateTransferResult;
import io.github.juankia.transfers.application.port.out.IdempotencyRecord;
import io.github.juankia.transfers.application.port.out.IdempotencyStore;
import io.github.juankia.transfers.application.port.out.OutboxPort;
import io.github.juankia.transfers.application.saga.TransferSagaOrchestrator;
import io.github.juankia.transfers.domain.model.Account;
import io.github.juankia.transfers.domain.model.Money;
import io.github.juankia.transfers.domain.model.Transfer;
import io.github.juankia.transfers.domain.model.TransferStatus;
import io.github.juankia.transfers.support.DirectTransactionRunner;
import io.github.juankia.transfers.support.InMemoryAccountRepository;
import io.github.juankia.transfers.support.InMemoryTransferRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InitiateTransferServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-01-15T10:00:00Z"), ZoneOffset.UTC);

    @Mock
    IdempotencyStore idempotencyStore;
    @Mock
    OutboxPort outbox;
    @Mock
    TransferSagaOrchestrator orchestrator;

    private final InMemoryAccountRepository accounts = new InMemoryAccountRepository();
    private final InMemoryTransferRepository transfers = new InMemoryTransferRepository();
    private final UUID payer = UUID.randomUUID();
    private final UUID payee = UUID.randomUUID();
    private InitiateTransferService service;

    @BeforeEach
    void setUp() {
        accounts.save(Account.open(payer, "alice", Money.of("100", "USD")));
        accounts.save(Account.open(payee, "bob", Money.of("0", "USD")));
        service = new InitiateTransferService(accounts, transfers, idempotencyStore, outbox, orchestrator,
                new DirectTransactionRunner(), CLOCK, UUID::randomUUID);
    }

    @Test
    void newKeyCreatesTransferAndRunsSaga() {
        when(idempotencyStore.find("alice", "key-00001")).thenReturn(Optional.empty());
        when(orchestrator.run(any())).thenAnswer(inv -> transfers.findById(inv.getArgument(0)).orElseThrow());

        InitiateTransferResult result = service.initiate(command("alice", "key-00001", "10"));

        assertThat(result.replayed()).isFalse();
        assertThat(result.transfer().status()).isEqualTo(TransferStatus.INITIATED);
        verify(idempotencyStore).register(any());
        verify(outbox).append(any());
    }

    @Test
    void sameKeyAndPayloadReplaysWithoutRunningSagaAgain() {
        InitiateTransferCommand cmd = command("alice", "key-00001", "10");
        Transfer existing = transfers.save(Transfer.initiate(UUID.randomUUID(), "alice", payer, payee,
                cmd.amount(), cmd.reference(), CLOCK.instant()));
        when(idempotencyStore.find("alice", "key-00001")).thenReturn(Optional.of(
                new IdempotencyRecord("alice", "key-00001", cmd.fingerprint(), existing.id(), CLOCK.instant())));

        InitiateTransferResult result = service.initiate(command("alice", "key-00001", "10.00"));

        assertThat(result.replayed()).isTrue();
        assertThat(result.transfer().id()).isEqualTo(existing.id());
        verify(orchestrator, never()).run(any());
    }

    @Test
    void sameKeyDifferentPayloadIsConflict() {
        when(idempotencyStore.find("alice", "key-00001")).thenReturn(Optional.of(
                new IdempotencyRecord("alice", "key-00001", "another-hash", UUID.randomUUID(), CLOCK.instant())));

        assertThatThrownBy(() -> service.initiate(command("alice", "key-00001", "99")))
                .isInstanceOf(IdempotencyKeyConflictException.class);
    }

    @Test
    void concurrentDuplicateLosingTheRaceReplaysTheWinner() {
        InitiateTransferCommand cmd = command("alice", "key-00001", "10");
        Transfer winner = transfers.save(Transfer.initiate(UUID.randomUUID(), "alice", payer, payee,
                cmd.amount(), cmd.reference(), CLOCK.instant()));
        when(idempotencyStore.find("alice", "key-00001"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(new IdempotencyRecord("alice", "key-00001", cmd.fingerprint(), winner.id(),
                        CLOCK.instant())));
        doThrow(new DuplicateIdempotencyKeyException("key-00001", null)).when(idempotencyStore).register(any());

        InitiateTransferResult result = service.initiate(cmd);

        assertThat(result.replayed()).isTrue();
        assertThat(result.transfer().id()).isEqualTo(winner.id());
        verify(orchestrator, never()).run(any());
    }

    @Test
    void onlyTheOwnerCanMoveMoneyFromAnAccount() {
        when(idempotencyStore.find(any(), any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.initiate(command("mallory", "key-00001", "10")))
                .isInstanceOf(AccountAccessDeniedException.class);
    }

    private InitiateTransferCommand command(String requester, String key, String amount) {
        return new InitiateTransferCommand(requester, key, payer, payee, Money.of(amount, "USD"), "dinner");
    }
}
