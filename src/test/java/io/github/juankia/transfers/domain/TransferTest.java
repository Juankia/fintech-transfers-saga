package io.github.juankia.transfers.domain;

import io.github.juankia.transfers.domain.event.TransferEvent;
import io.github.juankia.transfers.domain.exception.InvalidTransferException;
import io.github.juankia.transfers.domain.exception.InvalidTransferStateException;
import io.github.juankia.transfers.domain.model.CompensationAction;
import io.github.juankia.transfers.domain.model.FailureReason;
import io.github.juankia.transfers.domain.model.Money;
import io.github.juankia.transfers.domain.model.Transfer;
import io.github.juankia.transfers.domain.model.TransferStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TransferTest {

    private static final Instant NOW = Instant.parse("2026-01-15T10:00:00Z");

    private Transfer newTransfer() {
        return Transfer.initiate(UUID.randomUUID(), "alice", UUID.randomUUID(), UUID.randomUUID(),
                Money.of("25", "USD"), "rent", NOW);
    }

    @Test
    void happyPathRaisesEventsInOrder() {
        Transfer t = newTransfer();
        t.markFundsReserved(NOW);
        t.markRiskApproved(15, NOW);
        t.markDebited(NOW);
        t.complete(NOW);

        assertThat(t.status()).isEqualTo(TransferStatus.COMPLETED);
        assertThat(t.riskScore()).isEqualTo(15);
        assertThat(t.pullEvents()).extracting(TransferEvent::type).containsExactly(
                "TransferInitiated", "FundsReserved", "RiskApproved", "FundsDebited", "FundsCredited",
                "TransferCompleted");
        assertThat(t.pullEvents()).isEmpty();
    }

    @Test
    void cannotSkipSteps() {
        Transfer t = newTransfer();
        assertThatThrownBy(() -> t.markDebited(NOW)).isInstanceOf(InvalidTransferStateException.class);
        assertThatThrownBy(() -> t.complete(NOW)).isInstanceOf(InvalidTransferStateException.class);
    }

    @Test
    void sourceAndTargetMustDiffer() {
        UUID same = UUID.randomUUID();
        assertThatThrownBy(() -> Transfer.initiate(UUID.randomUUID(), "alice", same, same, Money.of("1", "USD"), null, NOW))
                .isInstanceOf(InvalidTransferException.class);
    }

    @Test
    void failureAfterDebitIsCompensatedByRefundAndEndsFailed() {
        Transfer t = newTransfer();
        t.markFundsReserved(NOW);
        t.markRiskApproved(10, NOW);
        t.markDebited(NOW);
        t.pullEvents();

        t.beginCompensation(FailureReason.CREDIT_FAILED, "beneficiary frozen", NOW);
        assertThat(t.compensationAction()).isEqualTo(CompensationAction.REFUND_DEBIT);
        t.finishCompensation(NOW);

        assertThat(t.status()).isEqualTo(TransferStatus.FAILED);
        assertThat(t.pullEvents()).extracting(TransferEvent::type)
                .containsExactly("CompensationStarted", "DebitReversed", "TransferFailed");
    }

    @Test
    void riskRejectionReleasesReservationAndEndsRejected() {
        Transfer t = newTransfer();
        t.markFundsReserved(NOW);
        t.beginCompensation(FailureReason.RISK_REJECTED, "score 80", NOW);

        assertThat(t.compensationAction()).isEqualTo(CompensationAction.RELEASE_RESERVATION);
        t.finishCompensation(NOW);
        assertThat(t.status()).isEqualTo(TransferStatus.REJECTED);
    }

    @Test
    void nothingToCompensateBeforeFundsAreReserved() {
        Transfer t = newTransfer();
        assertThatThrownBy(() -> t.beginCompensation(FailureReason.CREDIT_FAILED, "x", NOW))
                .isInstanceOf(InvalidTransferStateException.class);
    }
}
