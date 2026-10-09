package io.github.juankia.transfers.infrastructure.persistence;

import io.github.juankia.transfers.application.port.out.TransferRepository;
import io.github.juankia.transfers.domain.model.Money;
import io.github.juankia.transfers.domain.model.Transfer;
import io.github.juankia.transfers.domain.model.TransferStatus;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
class TransferPersistenceAdapter implements TransferRepository {

    private static final List<TransferStatus> IN_FLIGHT =
            Arrays.stream(TransferStatus.values()).filter(s -> !s.isTerminal()).toList();

    private final TransferJpaRepository repository;

    TransferPersistenceAdapter(TransferJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<Transfer> findById(UUID id) {
        return repository.findById(id).map(TransferPersistenceAdapter::toDomain);
    }

    @Override
    public Transfer save(Transfer t) {
        TransferEntity entity = new TransferEntity(t.id(), t.ownerId(), t.sourceAccountId(), t.targetAccountId(),
                t.amount().amount(), t.amount().currency().getCurrencyCode(), t.reference(), t.status(),
                t.failureReason(), t.failureDetail(), t.riskScore(), t.compensateFrom(), t.createdAt(),
                t.updatedAt(), t.version());
        return toDomain(repository.save(entity));
    }

    @Override
    public long countFromSourceSince(UUID sourceAccountId, Instant since, UUID excludeId) {
        return repository.countBySourceAccountIdAndCreatedAtGreaterThanEqualAndIdNot(sourceAccountId, since, excludeId);
    }

    @Override
    public boolean existsCompletedBetween(UUID sourceAccountId, UUID targetAccountId) {
        return repository.existsBySourceAccountIdAndTargetAccountIdAndStatus(sourceAccountId, targetAccountId,
                TransferStatus.COMPLETED);
    }

    @Override
    public List<UUID> findInFlightUpdatedBefore(Instant updatedBefore, int limit) {
        return repository.findIdsByStatusInAndUpdatedAtBefore(IN_FLIGHT, updatedBefore, PageRequest.of(0, limit));
    }

    private static Transfer toDomain(TransferEntity e) {
        return Transfer.rehydrate(e.getId(), e.getOwnerId(), e.getSourceAccountId(), e.getTargetAccountId(),
                Money.of(e.getAmount(), e.getCurrency()), e.getReference(), e.getStatus(), e.getFailureReason(),
                e.getFailureDetail(), e.getRiskScore(), e.getCompensateFrom(), e.getCreatedAt(), e.getUpdatedAt(),
                e.getVersion());
    }
}
