CREATE TABLE IF NOT EXISTS maintenance_history_evidence (
    maintenance_history_id BIGINT NOT NULL REFERENCES maintenance_history(id) ON DELETE CASCADE,
    url VARCHAR(500) NOT NULL
);
