package io.github.juankia.transfers.infrastructure.persistence;

import io.github.juankia.transfers.domain.model.TransferStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

interface TransferJpaRepository extends JpaRepository<TransferEntity, UUID> {

    long countBySourceAccountIdAndCreatedAtGreaterThanEqualAndIdNot(UUID sourceAccountId, Instant since, UUID excludeId);

    boolean existsBySourceAccountIdAndTargetAccountIdAndStatus(UUID sourceAccountId, UUID targetAccountId,
                                                               TransferStatus status);

    @Query("""
            select t.id from TransferEntity t
            where t.status in :statuses and t.updatedAt < :before
            order by t.updatedAt asc""")
    List<UUID> findIdsByStatusInAndUpdatedAtBefore(@Param("statuses") Collection<TransferStatus> statuses,
                                                   @Param("before") Instant before, Pageable page);
}
