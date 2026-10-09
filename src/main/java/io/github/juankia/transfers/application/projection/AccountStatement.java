package io.github.juankia.transfers.application.projection;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record AccountStatement(UUID accountId, List<StatementEntry> entries, BigDecimal netMovement, String currency) {
}
