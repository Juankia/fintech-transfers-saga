package io.github.juankia.transfers.infrastructure.risk;

import io.github.juankia.transfers.application.port.out.RiskScoringPort;
import io.github.juankia.transfers.application.port.out.TransferRepository;
import io.github.juankia.transfers.domain.model.RiskAssessment;
import io.github.juankia.transfers.domain.model.Transfer;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;

/**
 * Deterministic, explainable risk scoring. Each rule adds points and a named signal; the transfer
 * is rejected when the total reaches {@code rejectThreshold}. Swap this bean for an ML-backed or
 * vendor adapter without touching the saga.
 */
@Component
class RulesBasedRiskScoringAdapter implements RiskScoringPort {

    private final TransferRepository transfers;
    private final RiskRulesProperties rules;
    private final Clock clock;

    RulesBasedRiskScoringAdapter(TransferRepository transfers, RiskRulesProperties rules, Clock clock) {
        this.transfers = transfers;
        this.rules = rules;
        this.clock = clock;
    }

    @Override
    public RiskAssessment assess(Transfer transfer) {
        int score = 0;
        List<String> signals = new ArrayList<>();
        BigDecimal amount = transfer.amount().amount();

        if (amount.compareTo(rules.highAmount()) > 0) {
            score += rules.highAmountScore();
            signals.add("HIGH_AMOUNT");
        } else if (amount.compareTo(rules.elevatedAmount()) > 0) {
            score += rules.elevatedAmountScore();
            signals.add("ELEVATED_AMOUNT");
        }

        long recent = transfers.countFromSourceSince(transfer.sourceAccountId(),
                clock.instant().minus(rules.velocityWindow()), transfer.id());
        if (recent >= rules.velocityMaxTransfers()) {
            score += rules.velocityScore();
            signals.add("HIGH_VELOCITY");
        }

        if (!transfers.existsCompletedBetween(transfer.sourceAccountId(), transfer.targetAccountId())) {
            score += rules.newBeneficiaryScore();
            signals.add("NEW_BENEFICIARY");
        }

        int bounded = Math.min(score, 100);
        return new RiskAssessment(bounded, bounded < rules.rejectThreshold(), signals);
    }
}
