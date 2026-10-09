package io.github.juankia.transfers.domain.model;

/**
 * Why a transfer did not complete. Business rejections end in {@link TransferStatus#REJECTED};
 * everything else ends in {@link TransferStatus#FAILED} once compensations have run.
 */
public enum FailureReason {
    INSUFFICIENT_FUNDS(true),
    SOURCE_ACCOUNT_UNAVAILABLE(true),
    RISK_REJECTED(true),
    RISK_ASSESSMENT_UNAVAILABLE(false),
    DEBIT_FAILED(false),
    CREDIT_FAILED(false);

    private final boolean businessRejection;

    FailureReason(boolean businessRejection) {
        this.businessRejection = businessRejection;
    }

    public boolean isBusinessRejection() {
        return businessRejection;
    }
}
