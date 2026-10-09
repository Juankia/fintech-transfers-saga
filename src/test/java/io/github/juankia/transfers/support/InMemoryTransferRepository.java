package io.github.juankia.transfers.support;

import io.github.juankia.transfers.application.port.out.TransferRepository;
import io.github.juankia.transfers.domain.model.Transfer;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class InMemoryTransferRepository implements TransferRepository {

    private final Map<UUID, Transfer> rows = new HashMap<>();

    @Override
    public Optional<Transfer> findById(UUID id) {
        return Optional.ofNullable(rows.get(id)).map(t -> copy(t, t.version()));
    }

    @Override
    public Transfer save(Transfer t) {
        Transfer stored = copy(t, t.version() == null ? 0 : t.version() + 1);
        rows.put(t.id(), stored);
        return copy(stored, stored.version());
    }

    @Override
    public long countFromSourceSince(UUID sourceAccountId, Instant since, UUID excludeId) {
        return rows.values().stream()
                .filter(t -> t.sourceAccountId().equals(sourceAccountId) && !t.id().equals(excludeId)
                        && !t.createdAt().isBefore(since))
                .count();
    }

    @Override
    public boolean existsCompletedBetween(UUID sourceAccountId, UUID targetAccountId) {
        return false;
    }

    @Override
    public List<UUID> findInFlightUpdatedBefore(Instant updatedBefore, int limit) {
        return rows.values().stream()
                .filter(t -> !t.status().isTerminal() && t.updatedAt().isBefore(updatedBefore))
                .map(Transfer::id).limit(limit).toList();
    }

    private static Transfer copy(Transfer t, Long version) {
        return Transfer.rehydrate(t.id(), t.ownerId(), t.sourceAccountId(), t.targetAccountId(), t.amount(),
                t.reference(), t.status(), t.failureReason(), t.failureDetail(), t.riskScore(), t.compensateFrom(),
                t.createdAt(), t.updatedAt(), version);
    }
}
