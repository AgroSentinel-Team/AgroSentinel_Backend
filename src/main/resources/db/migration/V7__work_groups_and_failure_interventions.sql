CREATE TABLE IF NOT EXISTS work_groups (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    description TEXT,
    leader_id BIGINT REFERENCES usuarios(id) ON DELETE SET NULL,
    last_assigned_technician_id BIGINT REFERENCES usuarios(id) ON DELETE SET NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP WITHOUT TIME ZONE
);

ALTER TABLE assets
    ADD COLUMN IF NOT EXISTS inventory_code VARCHAR(80);

CREATE UNIQUE INDEX IF NOT EXISTS idx_assets_inventory_code
    ON assets (inventory_code)
    WHERE inventory_code IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS idx_work_groups_active_name
    ON work_groups (LOWER(name))
    WHERE deleted_at IS NULL;

CREATE TABLE IF NOT EXISTS work_group_members (
    group_id BIGINT NOT NULL REFERENCES work_groups(id) ON DELETE CASCADE,
    usuario_id BIGINT NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    PRIMARY KEY (group_id, usuario_id)
);

CREATE INDEX IF NOT EXISTS idx_work_group_members_usuario_id
    ON work_group_members (usuario_id, group_id);

CREATE TABLE IF NOT EXISTS work_group_roles (
    group_id BIGINT NOT NULL REFERENCES work_groups(id) ON DELETE CASCADE,
    role_name VARCHAR(40) NOT NULL,
    PRIMARY KEY (group_id, role_name)
);

ALTER TABLE failure_reports
    ADD COLUMN IF NOT EXISTS group_id BIGINT REFERENCES work_groups(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS assigned_technician_id BIGINT REFERENCES usuarios(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS fecha_inicio_revision TIMESTAMP WITHOUT TIME ZONE;

ALTER TABLE failure_reports
    DROP CONSTRAINT IF EXISTS failure_reports_estado_check;

ALTER TABLE failure_reports
    ADD CONSTRAINT failure_reports_estado_check
        CHECK (estado IN ('PENDIENTE', 'EN_REVISION', 'RESUELTO', 'ESPERA_REPUESTO'));

CREATE INDEX IF NOT EXISTS idx_failure_reports_group_status
    ON failure_reports (group_id, estado, fecha_reporte DESC);

CREATE INDEX IF NOT EXISTS idx_failure_reports_technician_status
    ON failure_reports (assigned_technician_id, estado, fecha_reporte DESC);


ALTER TABLE maintenance_history
    ADD COLUMN IF NOT EXISTS failure_report_id BIGINT UNIQUE REFERENCES failure_reports(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS equipment_intervened VARCHAR(1000),
    ADD COLUMN IF NOT EXISTS initial_reported_symptom TEXT,
    ADD COLUMN IF NOT EXISTS intervention_reported_at TIMESTAMP WITHOUT TIME ZONE,
    ADD COLUMN IF NOT EXISTS inspection_started_at TIMESTAMP WITHOUT TIME ZONE,
    ADD COLUMN IF NOT EXISTS actual_failure TEXT,
    ADD COLUMN IF NOT EXISTS root_cause TEXT,
    ADD COLUMN IF NOT EXISTS work_minutes INTEGER,
    ADD COLUMN IF NOT EXISTS tests_performed TEXT,
    ADD COLUMN IF NOT EXISTS final_equipment_state VARCHAR(40),
    ADD COLUMN IF NOT EXISTS recommendations TEXT,
    ADD COLUMN IF NOT EXISTS pending_tasks TEXT,
    ADD COLUMN IF NOT EXISTS received_by VARCHAR(160);

CREATE TABLE IF NOT EXISTS maintenance_history_parts (
    maintenance_history_id BIGINT NOT NULL REFERENCES maintenance_history(id) ON DELETE CASCADE,
    part_number VARCHAR(120) NOT NULL,
    part_name VARCHAR(160) NOT NULL,
    part_brand VARCHAR(120),
    part_quantity NUMERIC(14, 3) NOT NULL CHECK (part_quantity > 0)
);
