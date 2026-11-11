ALTER TABLE assets
    ADD COLUMN IF NOT EXISTS current_horometer NUMERIC(12, 2) NOT NULL DEFAULT 0;

ALTER TABLE maintenance_history
    ADD COLUMN IF NOT EXISTS horometer_at_maintenance NUMERIC(12, 2);

ALTER TABLE preventive_maintenances
    ADD COLUMN IF NOT EXISTS horometer_at_execution NUMERIC(12, 2);
