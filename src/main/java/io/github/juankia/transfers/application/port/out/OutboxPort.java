package io.github.juankia.transfers.application.port.out;

import io.github.juankia.transfers.domain.event.TransferEvent;

/**
 * Stores an event in the transactional outbox. Must be called inside the transaction that
 * persists the corresponding state change; a separate relay publishes it to the broker.
 */
public interface OutboxPort {

    void append(TransferEvent event);
}
