-- Write model: aggregates + saga state
CREATE TABLE accounts (
    id          UUID PRIMARY KEY,
    owner_id    VARCHAR(64)    NOT NULL,
    currency    VARCHAR(3)      NOT NULL,
    balance     NUMERIC(19, 4) NOT NULL,
    reserved    NUMERIC(19, 4) NOT NULL DEFAULT 0,
    status      VARCHAR(16)    NOT NULL,
    version     BIGINT         NOT NULL DEFAULT 0,
    CONSTRAINT ck_accounts_reserved_non_negative CHECK (reserved >= 0)
);

CREATE INDEX ix_accounts_owner ON accounts (owner_id);

CREATE TABLE transfers (
    id                 UUID PRIMARY KEY,
    owner_id           VARCHAR(64)    NOT NULL,
    source_account_id  UUID           NOT NULL REFERENCES accounts (id),
    target_account_id  UUID           NOT NULL REFERENCES accounts (id),
    amount             NUMERIC(19, 4) NOT NULL,
    currency           VARCHAR(3)      NOT NULL,
    reference          VARCHAR(140),
    status             VARCHAR(32)    NOT NULL,
    failure_reason     VARCHAR(64),
    failure_detail     VARCHAR(512),
    risk_score         INTEGER,
    compensate_from    VARCHAR(32),
    created_at         TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at         TIMESTAMP WITH TIME ZONE NOT NULL,
    version            BIGINT         NOT NULL DEFAULT 0
);

CREATE INDEX ix_transfers_source_created ON transfers (source_account_id, created_at);
CREATE INDEX ix_transfers_status_updated ON transfers (status, updated_at);

-- Idempotency-Key registry, scoped per caller
CREATE TABLE idempotency_keys (
    owner_id         VARCHAR(64)  NOT NULL,
    idempotency_key  VARCHAR(64)  NOT NULL,
    request_hash     VARCHAR(64)     NOT NULL,
    transfer_id      UUID         NOT NULL REFERENCES transfers (id),
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_idempotency_keys PRIMARY KEY (owner_id, idempotency_key)
);
