package io.github.juankia.transfers.infrastructure.persistence;

import io.github.juankia.transfers.application.port.out.StatementRepository;
import io.github.juankia.transfers.application.projection.StatementEntry;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

@Component
class StatementRepositoryAdapter implements StatementRepository {

    private final StatementEntryJpaRepository repository;

    StatementRepositoryAdapter(StatementEntryJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public void append(StatementEntry entry) {
        repository.save(new StatementEntryEntity(entry));
    }

    @Override
    public List<StatementEntry> findByAccount(UUID accountId) {
        return repository.findByAccountIdOrderByOccurredAtAscEventIdAsc(accountId).stream()
                .map(StatementEntryEntity::toEntry)
                .map(StatementRepositoryAdapter::normaliseScale)
                .toList();
    }

    private static StatementEntry normaliseScale(StatementEntry e) {
        BigDecimal amount = e.amount().setScale(Currency.getInstance(e.currency()).getDefaultFractionDigits());
        return new StatementEntry(e.eventId(), e.accountId(), e.transferId(), e.type(), amount, e.currency(),
                e.occurredAt());
    }
}
