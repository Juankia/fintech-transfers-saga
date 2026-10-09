package io.github.juankia.transfers.domain.model;

public enum CompensationAction {
    /** Funds were only held: drop the hold. */
    RELEASE_RESERVATION,
    /** Funds already left the payer: put them back. */
    REFUND_DEBIT
}
