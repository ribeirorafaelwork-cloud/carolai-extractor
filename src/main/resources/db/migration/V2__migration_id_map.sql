CREATE TABLE migration_id_map (
    id            BIGSERIAL PRIMARY KEY,
    entity_type   VARCHAR(30)  NOT NULL,
    source_key    VARCHAR(255) NOT NULL,
    platform_id   VARCHAR(64)  NOT NULL,
    payload_hash  VARCHAR(64),
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_migration_id_map UNIQUE (entity_type, source_key)
);

CREATE TABLE migration_run (
    id            BIGSERIAL PRIMARY KEY,
    run_type      VARCHAR(30)  NOT NULL,
    status        VARCHAR(20)  NOT NULL DEFAULT 'RUNNING',
    total         INT          NOT NULL DEFAULT 0,
    migrated      INT          NOT NULL DEFAULT 0,
    skipped       INT          NOT NULL DEFAULT 0,
    failed        INT          NOT NULL DEFAULT 0,
    dry_run       BOOLEAN      NOT NULL DEFAULT FALSE,
    started_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    finished_at   TIMESTAMPTZ,
    error_log     TEXT
);
