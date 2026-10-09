package io.github.juankia.transfers.domain.model;

import java.util.List;

/**
 * Outcome of a fraud / risk evaluation. {@code score} is 0 (no risk) to 100 (certain fraud).
 */
public record RiskAssessment(int score, boolean approved, List<String> signals) {

    public RiskAssessment {
        if (score < 0 || score > 100) {
            throw new IllegalArgumentException("Risk score must be between 0 and 100");
        }
        signals = List.copyOf(signals);
    }
}
