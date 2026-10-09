package io.github.juankia.transfers.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface AccountJpaRepository extends JpaRepository<AccountEntity, UUID> {
}
