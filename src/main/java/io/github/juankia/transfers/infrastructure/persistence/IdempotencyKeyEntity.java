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
@Table(name = "idempotency_keys")
class IdempotencyKeyEntity implements Persistable<IdempotencyKeyEntity.Key> {

    @Embeddable
    record Key(@Column(name = "owner_id") String ownerId,
               @Column(name = "idempotency_key") String idempotencyKey) implements Serializable {
    }

    @EmbeddedId
    private Key id;
    @Column(name = "request_hash", nullable = false, length = 64)
    private String requestHash;
    @Column(name = "transfer_id", nullable = false)
    private UUID transferId;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Transient
    private boolean isNew;

    protected IdempotencyKeyEntity() {
    }

    IdempotencyKeyEntity(Key id, String requestHash, UUID transferId, Instant createdAt) {
        this.id = id;
        this.requestHash = requestHash;
        this.transferId = transferId;
        this.createdAt = createdAt;
        this.isNew = true;
    }

    @Override
    public Key getId() { return id; }

    @Override
    public boolean isNew() { return isNew; }

    String getRequestHash() { return requestHash; }
    UUID getTransferId() { return transferId; }
    Instant getCreatedAt() { return createdAt; }
}
