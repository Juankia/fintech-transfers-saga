package io.github.juankia.transfers.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

interface IdempotencyKeyJpaRepository extends JpaRepository<IdempotencyKeyEntity, IdempotencyKeyEntity.Key> {
}
