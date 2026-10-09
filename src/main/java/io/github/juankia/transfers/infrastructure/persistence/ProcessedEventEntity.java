package io.github.juankia.transfers.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import org.springframework.data.domain.Persistable;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "processed_events")
class ProcessedEventEntity implements Persistable<ProcessedEventEntity.Key> {

    @Embeddable
    record Key(@Column(name = "event_id") UUID eventId, @Column(name = "consumer") String consumer)
            implements Serializable {
    }

    @EmbeddedId
    private Key id;
    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;
    @Transient
    private boolean isNew;

    protected ProcessedEventEntity() {
    }

    ProcessedEventEntity(Key id, Instant processedAt) {
        this.id = id;
        this.processedAt = processedAt;
        this.isNew = true;
    }

    @Override
    public Key getId() { return id; }

    @Override
    public boolean isNew() { return isNew; }
}
