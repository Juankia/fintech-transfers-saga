package io.github.juankia.transfers.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

interface ProcessedEventJpaRepository extends JpaRepository<ProcessedEventEntity, ProcessedEventEntity.Key> {
}
