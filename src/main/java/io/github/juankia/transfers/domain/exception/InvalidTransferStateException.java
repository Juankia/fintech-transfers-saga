package io.github.juankia.transfers.domain.exception;

import io.github.juankia.transfers.domain.model.TransferStatus;

import java.util.UUID;

public class InvalidTransferStateException extends DomainException {

    public InvalidTransferStateException(UUID transferId, TransferStatus current, String operation) {
        super("Transfer %s cannot '%s' while in status %s".formatted(transferId, operation, current));
    }
}
