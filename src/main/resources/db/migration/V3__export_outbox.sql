CREATE TABLE export_outbox (
    id            BIGSERIAL PRIMARY KEY,
    entity_type   VARCHAR(30)  NOT NULL,
    source_key    VARCHAR(255) NOT NULL,
    payload       JSONB        NOT NULL,
    payload_hash  VARCHAR(64)  NOT NULL,
    status        VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    acked_at      TIMESTAMPTZ,
    error_msg     TEXT,
    CONSTRAINT uq_outbox_entity_source UNIQUE (entity_type, source_key)
);

CREATE INDEX idx_outbox_status      ON export_outbox (status);
CREATE INDEX idx_outbox_type_status ON export_outbox (entity_type, status);
