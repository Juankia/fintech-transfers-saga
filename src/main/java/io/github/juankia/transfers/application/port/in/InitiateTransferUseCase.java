package io.github.juankia.transfers.application.port.in;

public interface InitiateTransferUseCase {

    InitiateTransferResult initiate(InitiateTransferCommand command);
}
