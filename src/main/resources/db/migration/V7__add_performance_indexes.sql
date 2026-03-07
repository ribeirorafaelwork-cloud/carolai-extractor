-- Performance indexes for extractor production readiness

-- export_outbox: source_key lookups (used in findByEntityTypeAndSourceKey)
CREATE INDEX IF NOT EXISTS idx_outbox_source_key
    ON export_outbox (entity_type, source_key);

-- export_outbox: time-range ordering (used in dashboard/listing)
CREATE INDEX IF NOT EXISTS idx_outbox_created_at
    ON export_outbox (created_at DESC);

-- customer: external_ref index (unique constraint exists but explicit index helps)
CREATE INDEX IF NOT EXISTS idx_customer_external_ref
    ON customer (external_ref);

-- physical_assessment: external_ref lookups
CREATE INDEX IF NOT EXISTS idx_physical_assessment_external_ref
    ON physical_assessment (external_ref);
