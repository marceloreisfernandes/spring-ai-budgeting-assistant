CREATE TABLE transactions (
    id              UUID PRIMARY KEY,
    description     VARCHAR(140) NOT NULL,
    amount_in_cents BIGINT NOT NULL CHECK (amount_in_cents > 0),
    category        VARCHAR(32) NOT NULL,
    source          VARCHAR(16) NOT NULL,
    owner_username  VARCHAR(100) NOT NULL,
    request_id      VARCHAR(100) NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_transactions_request_id UNIQUE (request_id)
);

CREATE INDEX idx_transactions_owner_category ON transactions (owner_username, category);
CREATE INDEX idx_transactions_created_at ON transactions (created_at);
