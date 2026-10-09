package io.github.juankia.transfers.domain.model;

/**
 * Saga state. Each non-terminal status identifies exactly which step runs next, which is what
 * makes the orchestrator resumable after a crash.
 */
public enum TransferStatus {
    INITIATED,
    FUNDS_RESERVED,
    RISK_APPROVED,
    DEBITED,
    COMPENSATING,
    COMPLETED(true),
    REJECTED(true),
    FAILED(true);

    private final boolean terminal;

    TransferStatus() {
        this(false);
    }

    TransferStatus(boolean terminal) {
        this.terminal = terminal;
    }

    public boolean isTerminal() {
        return terminal;
    }
}
