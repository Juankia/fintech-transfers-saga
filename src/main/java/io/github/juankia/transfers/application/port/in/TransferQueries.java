package io.github.juankia.transfers.application.port.in;

import io.github.juankia.transfers.domain.model.Transfer;

import java.util.UUID;

public interface TransferQueries {

    Transfer getTransfer(UUID transferId, String requesterId);
}
