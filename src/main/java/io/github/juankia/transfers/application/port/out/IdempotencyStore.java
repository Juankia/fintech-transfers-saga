package io.github.juankia.transfers.application.port.out;

import io.github.juankia.transfers.application.exception.DuplicateIdempotencyKeyException;

import java.util.Optional;

public interface IdempotencyStore {

    Optional<IdempotencyRecord> find(String ownerId, String key);

    /**
     * @throws DuplicateIdempotencyKeyException if (ownerId, key) is already registered
     */
    void register(IdempotencyRecord record);
}
