package io.github.juankia.transfers.application.port.out;

import io.github.juankia.transfers.domain.model.RiskAssessment;
import io.github.juankia.transfers.domain.model.Transfer;

/**
 * Fraud / risk engine. The default adapter is rules-based; an ML model or a third-party
 * provider can be plugged in by implementing this port.
 */
public interface RiskScoringPort {

    RiskAssessment assess(Transfer transfer);
}
