package io.github.juankia.transfers.application.saga;

import io.github.juankia.transfers.application.port.out.OutboxPort;
import io.github.juankia.transfers.application.port.out.RiskScoringPort;
import io.github.juankia.transfers.domain.event.TransferEvent;
import io.github.juankia.transfers.domain.model.Account;
import io.github.juankia.transfers.domain.model.FailureReason;
import io.github.juankia.transfers.domain.model.Money;
import io.github.juankia.transfers.domain.model.RiskAssessment;
import io.github.juankia.transfers.domain.model.Transfer;
import io.github.juankia.transfers.domain.model.TransferStatus;
import io.github.juankia.transfers.support.DirectTransactionRunner;
import io.github.juankia.transfers.support.InMemoryAccountRepository;
import io.github.juankia.transfers.support.InMemoryTransferRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransferSagaOrchestratorTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-01-15T10:00:00Z"), ZoneOffset.UTC);

    @Mock
    RiskScoringPort risk;
    @Mock
    OutboxPort outbox;

    private InMemoryAccountRepository accounts;
    private InMemoryTransferRepository transfers;
    private TransferSagaOrchestrator saga;

    private final UUID payer = UUID.randomUUID();
    private final UUID payee = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        accounts = new InMemoryAccountRepository();
        transfers = new InMemoryTransferRepository();
        saga = new TransferSagaOrchestrator(transfers, accounts, risk, outbox, new DirectTransactionRunner(), CLOCK);
        accounts.save(Account.open(payer, "alice", usd("500")));
        accounts.save(Account.open(payee, "bob", usd("0")));
    }

    @Test
    @DisplayName("happy path: reserve -> risk -> debit -> credit -> COMPLETED")
    void completesTransfer() {
        when(risk.assess(any())).thenReturn(new RiskAssessment(15, true, List.of("NEW_BENEFICIARY")));

        Transfer result = saga.run(newTransfer("120"));

        assertThat(result.status()).isEqualTo(TransferStatus.COMPLETED);
        assertThat(result.riskScore()).isEqualTo(15);
        assertThat(balance(payer)).isEqualTo(usd("380"));
        assertThat(reserved(payer)).isEqualTo(usd("0"));
        assertThat(balance(payee)).isEqualTo(usd("120"));
        assertThat(publishedEventTypes()).containsExactly(
                "FundsReserved", "RiskApproved", "FundsDebited", "FundsCredited", "TransferCompleted");
    }

    @Test
    @DisplayName("insufficient funds: rejected at step 1, nothing to compensate, risk never called")
    void rejectsWhenFundsAreInsufficient() {
        Transfer result = saga.run(newTransfer("500.01"));

        assertThat(result.status()).isEqualTo(TransferStatus.REJECTED);
        assertThat(result.failureReason()).isEqualTo(FailureReason.INSUFFICIENT_FUNDS);
        assertThat(reserved(payer)).isEqualTo(usd("0"));
        verify(risk, never()).assess(any());
        assertThat(publishedEventTypes()).containsExactly("TransferRejected");
    }

    @Test
    @DisplayName("risk rejection: compensation releases the reservation")
    void releasesReservationWhenRiskRejects() {
        when(risk.assess(any())).thenReturn(new RiskAssessment(85, false, List.of("HIGH_AMOUNT", "NEW_BENEFICIARY")));

        Transfer result = saga.run(newTransfer("300"));

        assertThat(result.status()).isEqualTo(TransferStatus.REJECTED);
        assertThat(result.failureReason()).isEqualTo(FailureReason.RISK_REJECTED);
        assertThat(result.riskScore()).isEqualTo(85);
        assertThat(result.failureDetail()).contains("HIGH_AMOUNT");
        assertThat(balance(payer)).isEqualTo(usd("500"));
        assertThat(reserved(payer)).isEqualTo(usd("0"));
        assertThat(publishedEventTypes()).containsExactly(
                "FundsReserved", "CompensationStarted", "ReservationReleased", "TransferRejected");
    }

    @Test
    @DisplayName("risk engine down: fail closed and release the reservation")
    void failsClosedWhenRiskEngineIsUnavailable() {
        when(risk.assess(any())).thenThrow(new IllegalStateException("timeout"));

        Transfer result = saga.run(newTransfer("50"));

        assertThat(result.status()).isEqualTo(TransferStatus.FAILED);
        assertThat(result.failureReason()).isEqualTo(FailureReason.RISK_ASSESSMENT_UNAVAILABLE);
        assertThat(reserved(payer)).isEqualTo(usd("0"));
    }

    @Test
    @DisplayName("credit fails after debit: compensation refunds the payer")
    void refundsPayerWhenCreditFails() {
        when(risk.assess(any())).thenReturn(new RiskAssessment(0, true, List.of()));
        Account frozen = accounts.findById(payee).orElseThrow();
        frozen.freeze();
        accounts.save(frozen);

        Transfer result = saga.run(newTransfer("200"));

        assertThat(result.status()).isEqualTo(TransferStatus.FAILED);
        assertThat(result.failureReason()).isEqualTo(FailureReason.CREDIT_FAILED);
        assertThat(balance(payer)).isEqualTo(usd("500"));
        assertThat(reserved(payer)).isEqualTo(usd("0"));
        assertThat(balance(payee)).isEqualTo(usd("0"));
        assertThat(publishedEventTypes()).containsExactly(
                "FundsReserved", "RiskApproved", "FundsDebited", "CompensationStarted", "DebitReversed",
                "TransferFailed");
    }

    @Test
    @DisplayName("resumes a saga persisted mid-flight (crash recovery)")
    void resumesFromPersistedStatus() {
        when(risk.assess(any())).thenReturn(new RiskAssessment(0, true, List.of()));
        UUID id = newTransfer("100");
        // Simulate a crash right after step 1 committed.
        Transfer t = transfers.findById(id).orElseThrow();
        Account a = accounts.findById(payer).orElseThrow();
        a.reserve(t.amount());
        accounts.save(a);
        t.markFundsReserved(CLOCK.instant());
        transfers.save(t);

        Transfer result = saga.run(id);

        assertThat(result.status()).isEqualTo(TransferStatus.COMPLETED);
        assertThat(balance(payer)).isEqualTo(usd("400"));
        assertThat(balance(payee)).isEqualTo(usd("100"));
    }

    @Test
    void runningACompletedSagaAgainIsANoOp() {
        when(risk.assess(any())).thenReturn(new RiskAssessment(0, true, List.of()));
        UUID id = newTransfer("10");
        saga.run(id);

        Transfer again = saga.run(id);

        assertThat(again.status()).isEqualTo(TransferStatus.COMPLETED);
        assertThat(balance(payer)).isEqualTo(usd("490"));
    }

    private UUID newTransfer(String amount) {
        Transfer t = Transfer.initiate(UUID.randomUUID(), "alice", payer, payee, usd(amount), "test", CLOCK.instant());
        t.pullEvents();
        return transfers.save(t).id();
    }

    private List<String> publishedEventTypes() {
        ArgumentCaptor<TransferEvent> captor = ArgumentCaptor.forClass(TransferEvent.class);
        verify(outbox, atLeastOnce()).append(captor.capture());
        return captor.getAllValues().stream().map(TransferEvent::type).toList();
    }

    private Money balance(UUID id) {
        return accounts.findById(id).orElseThrow().balance();
    }

    private Money reserved(UUID id) {
        return accounts.findById(id).orElseThrow().reserved();
    }

    private static Money usd(String amount) {
        return Money.of(amount, "USD");
    }
}
