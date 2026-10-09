package io.github.juankia.transfers.infrastructure.persistence;

import io.github.juankia.transfers.domain.model.FailureReason;
import io.github.juankia.transfers.domain.model.TransferStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "transfers")
public class TransferEntity {

    @Id
    private UUID id;
    @Column(name = "owner_id", nullable = false)
    private String ownerId;
    @Column(name = "source_account_id", nullable = false)
    private UUID sourceAccountId;
    @Column(name = "target_account_id", nullable = false)
    private UUID targetAccountId;
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;
    @Column(nullable = false, length = 3)
    private String currency;
    @Column(length = 140)
    private String reference;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private TransferStatus status;
    @Enumerated(EnumType.STRING)
    @Column(name = "failure_reason", length = 64)
    private FailureReason failureReason;
    @Column(name = "failure_detail", length = 512)
    private String failureDetail;
    @Column(name = "risk_score")
    private Integer riskScore;
    @Enumerated(EnumType.STRING)
    @Column(name = "compensate_from", length = 32)
    private TransferStatus compensateFrom;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    @Version
    private Long version;

    protected TransferEntity() {
    }

    TransferEntity(UUID id, String ownerId, UUID sourceAccountId, UUID targetAccountId, BigDecimal amount,
                   String currency, String reference, TransferStatus status, FailureReason failureReason,
                   String failureDetail, Integer riskScore, TransferStatus compensateFrom, Instant createdAt,
                   Instant updatedAt, Long version) {
        this.id = id;
        this.ownerId = ownerId;
        this.sourceAccountId = sourceAccountId;
        this.targetAccountId = targetAccountId;
        this.amount = amount;
        this.currency = currency;
        this.reference = reference;
        this.status = status;
        this.failureReason = failureReason;
        this.failureDetail = failureDetail;
        this.riskScore = riskScore;
        this.compensateFrom = compensateFrom;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.version = version;
    }

    UUID getId() { return id; }
    String getOwnerId() { return ownerId; }
    UUID getSourceAccountId() { return sourceAccountId; }
    UUID getTargetAccountId() { return targetAccountId; }
    BigDecimal getAmount() { return amount; }
    String getCurrency() { return currency; }
    String getReference() { return reference; }
    TransferStatus getStatus() { return status; }
    FailureReason getFailureReason() { return failureReason; }
    String getFailureDetail() { return failureDetail; }
    Integer getRiskScore() { return riskScore; }
    TransferStatus getCompensateFrom() { return compensateFrom; }
    Instant getCreatedAt() { return createdAt; }
    Instant getUpdatedAt() { return updatedAt; }
    Long getVersion() { return version; }
}
