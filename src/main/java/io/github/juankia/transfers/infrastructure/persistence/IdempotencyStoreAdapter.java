package io.github.juankia.transfers.infrastructure.persistence;

import io.github.juankia.transfers.application.exception.DuplicateIdempotencyKeyException;
import io.github.juankia.transfers.application.port.out.IdempotencyRecord;
import io.github.juankia.transfers.application.port.out.IdempotencyStore;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
class IdempotencyStoreAdapter implements IdempotencyStore {

    private final IdempotencyKeyJpaRepository repository;

    IdempotencyStoreAdapter(IdempotencyKeyJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<IdempotencyRecord> find(String ownerId, String key) {
        return repository.findById(new IdempotencyKeyEntity.Key(ownerId, key))
                .map(e -> new IdempotencyRecord(ownerId, key, e.getRequestHash(), e.getTransferId(), e.getCreatedAt()));
    }

    @Override
    public void register(IdempotencyRecord record) {
        try {
            // Flush now so a primary-key clash surfaces here, inside the adapter, and can be
            // translated into the application's own exception.
            repository.saveAndFlush(new IdempotencyKeyEntity(
                    new IdempotencyKeyEntity.Key(record.ownerId(), record.key()),
                    record.requestHash(), record.transferId(), record.createdAt()));
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateIdempotencyKeyException(record.key(), e);
        }
    }
}
