package io.github.juankia.transfers.infrastructure.outbox;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.QueryHints;

import java.util.List;
import java.util.UUID;

public interface OutboxEventJpaRepository extends JpaRepository<OutboxEventEntity, Long> {

    /**
     * Oldest unpublished events. On PostgreSQL this becomes {@code FOR UPDATE SKIP LOCKED}, so several
     * relay instances can poll concurrently without publishing the same row twice.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2"))
    List<OutboxEventEntity> findTop100ByPublishedAtIsNullOrderByIdAsc();

    List<OutboxEventEntity> findByAggregateIdOrderByIdAsc(UUID aggregateId);

    long countByPublishedAtIsNull();
}
