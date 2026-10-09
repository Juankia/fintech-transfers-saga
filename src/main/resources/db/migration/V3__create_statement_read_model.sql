-- CQRS read model: denormalised, append-only account statement
CREATE TABLE account_statement_entries (
    event_id     UUID PRIMARY KEY,
    account_id   UUID           NOT NULL,
    transfer_id  UUID           NOT NULL,
    entry_type   VARCHAR(16)    NOT NULL,
    amount       NUMERIC(19, 4) NOT NULL,
    currency     VARCHAR(3)      NOT NULL,
    occurred_at  TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX ix_statement_account_time ON account_statement_entries (account_id, occurred_at);
