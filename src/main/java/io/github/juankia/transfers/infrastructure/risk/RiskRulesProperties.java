package io.github.juankia.transfers.infrastructure.risk;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.math.BigDecimal;
import java.time.Duration;

/**
 * Tunable thresholds for the rules-based risk engine (amounts are in the transfer currency).
 */
@ConfigurationProperties(prefix = "transfers.risk")
public record RiskRulesProperties(
        @DefaultValue("2000") BigDecimal elevatedAmount,
        @DefaultValue("25") int elevatedAmountScore,
        @DefaultValue("10000") BigDecimal highAmount,
        @DefaultValue("60") int highAmountScore,
        @DefaultValue("10m") Duration velocityWindow,
        @DefaultValue("5") int velocityMaxTransfers,
        @DefaultValue("40") int velocityScore,
        @DefaultValue("15") int newBeneficiaryScore,
        @DefaultValue("70") int rejectThreshold) {
}
