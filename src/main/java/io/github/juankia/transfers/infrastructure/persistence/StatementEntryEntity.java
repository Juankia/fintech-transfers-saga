package io.github.juankia.transfers.infrastructure.persistence;

import io.github.juankia.transfers.application.projection.StatementEntry;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import org.springframework.data.domain.Persistable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "account_statement_entries")
class StatementEntryEntity implements Persistable<UUID> {

    @Id
    @Column(name = "event_id")
    private UUID eventId;
    @Column(name = "account_id", nullable = false)
    private UUID accountId;
    @Column(name = "transfer_id", nullable = false)
    private UUID transferId;
    @Enumerated(EnumType.STRING)
    @Column(name = "entry_type", nullable = false, length = 16)
    private StatementEntry.EntryType entryType;
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;
    @Column(nullable = false, length = 3)
    private String currency;
    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;
    @Transient
    private boolean isNew;

    protected StatementEntryEntity() {
    }

    StatementEntryEntity(StatementEntry e) {
        this.eventId = e.eventId();
        this.accountId = e.accountId();
        this.transferId = e.transferId();
        this.entryType = e.type();
        this.amount = e.amount();
        this.currency = e.currency();
        this.occurredAt = e.occurredAt();
        this.isNew = true;
    }

    StatementEntry toEntry() {
        return new StatementEntry(eventId, accountId, transferId, entryType, amount, currency, occurredAt);
    }

    @Override
    public UUID getId() { return eventId; }

    @Override
    public boolean isNew() { return isNew; }
}
