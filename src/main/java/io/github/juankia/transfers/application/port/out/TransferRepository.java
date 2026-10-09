package io.github.juankia.transfers.application.port.out;

import io.github.juankia.transfers.domain.model.Transfer;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TransferRepository {

    Optional<Transfer> findById(UUID id);

    Transfer save(Transfer transfer);

    /** Transfers created from {@code sourceAccountId} since {@code since}, excluding {@code excludeId}. */
    long countFromSourceSince(UUID sourceAccountId, Instant since, UUID excludeId);

    boolean existsCompletedBetween(UUID sourceAccountId, UUID targetAccountId);

    /** Non-terminal transfers not touched since {@code updatedBefore}: candidates for saga recovery. */
    List<UUID> findInFlightUpdatedBefore(Instant updatedBefore, int limit);
}
