package io.github.juankia.transfers.infrastructure.persistence;

import io.github.juankia.transfers.domain.model.AccountStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "accounts")
public class AccountEntity {

    @Id
    private UUID id;

    @Column(name = "owner_id", nullable = false)
    private String ownerId;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal balance;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal reserved;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private AccountStatus status;

    @Version
    private Long version;

    protected AccountEntity() {
    }

    public AccountEntity(UUID id, String ownerId, String currency, BigDecimal balance, BigDecimal reserved,
                         AccountStatus status, Long version) {
        this.id = id;
        this.ownerId = ownerId;
        this.currency = currency;
        this.balance = balance;
        this.reserved = reserved;
        this.status = status;
        this.version = version;
    }

    public UUID getId() { return id; }
    public String getOwnerId() { return ownerId; }
    public String getCurrency() { return currency; }
    public BigDecimal getBalance() { return balance; }
    public BigDecimal getReserved() { return reserved; }
    public AccountStatus getStatus() { return status; }
    public Long getVersion() { return version; }
}
