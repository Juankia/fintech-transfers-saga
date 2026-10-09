package io.github.juankia.transfers.application.exception;

import java.util.UUID;

public class TransferNotFoundException extends RuntimeException {

    public TransferNotFoundException(UUID transferId) {
        super("Transfer %s not found".formatted(transferId));
    }
}
