package io.github.juankia.transfers.infrastructure.risk;

import io.github.juankia.transfers.application.port.out.TransferRepository;
import io.github.juankia.transfers.domain.model.Money;
import io.github.juankia.transfers.domain.model.RiskAssessment;
import io.github.juankia.transfers.domain.model.Transfer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RulesBasedRiskScoringAdapterTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-01-15T10:00:00Z"), ZoneOffset.UTC);
    private static final RiskRulesProperties RULES = new RiskRulesProperties(new BigDecimal("2000"), 25,
            new BigDecimal("10000"), 60, Duration.ofMinutes(10), 5, 40, 15, 70);

    @Mock
    TransferRepository transfers;

    private RiskAssessment assess(String amount, long recentTransfers, boolean knownBeneficiary) {
        when(transfers.countFromSourceSince(any(), any(), any())).thenReturn(recentTransfers);
        when(transfers.existsCompletedBetween(any(), any())).thenReturn(knownBeneficiary);
        Transfer t = Transfer.initiate(UUID.randomUUID(), "alice", UUID.randomUUID(), UUID.randomUUID(),
                Money.of(amount, "USD"), null, CLOCK.instant());
        return new RulesBasedRiskScoringAdapter(transfers, RULES, CLOCK).assess(t);
    }

    @Test
    void smallPaymentToKnownBeneficiaryIsClean() {
        RiskAssessment a = assess("50", 0, true);
        assertThat(a.score()).isZero();
        assertThat(a.approved()).isTrue();
        assertThat(a.signals()).isEmpty();
    }

    @Test
    void newBeneficiaryAloneIsApproved() {
        RiskAssessment a = assess("50", 0, false);
        assertThat(a.score()).isEqualTo(15);
        assertThat(a.approved()).isTrue();
    }

    @Test
    void highAmountToNewBeneficiaryIsRejected() {
        RiskAssessment a = assess("15000", 0, false);
        assertThat(a.score()).isEqualTo(75);
        assertThat(a.approved()).isFalse();
        assertThat(a.signals()).containsExactly("HIGH_AMOUNT", "NEW_BENEFICIARY");
    }

    @Test
    void velocityPlusElevatedAmountIsRejected() {
        RiskAssessment a = assess("2500", 5, true);
        assertThat(a.score()).isEqualTo(65);
        assertThat(a.approved()).isTrue();

        RiskAssessment b = assess("2500", 5, false);
        assertThat(b.score()).isEqualTo(80);
        assertThat(b.approved()).isFalse();
        assertThat(b.signals()).contains("HIGH_VELOCITY");
    }
}
