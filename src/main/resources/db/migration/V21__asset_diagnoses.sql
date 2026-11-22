-- Fichas técnicas de diagnóstico del estado de los activos y sus necesidades identificadas.

CREATE TABLE IF NOT EXISTS asset_diagnoses (
    id BIGSERIAL PRIMARY KEY,
    asset_id BIGINT NOT NULL REFERENCES assets (id) ON DELETE CASCADE,
    diagnosed_on DATE NOT NULL,
    inspector_id BIGINT REFERENCES usuarios (id) ON DELETE SET NULL,
    inspector_name VARCHAR(255),
    current_state_description TEXT,
    technical_diagnosis TEXT,
    technical_concept TEXT,
    conclusion TEXT,
    operational_condition VARCHAR(40) NOT NULL,
    recommendation VARCHAR(40) NOT NULL,
    recommendation_detail TEXT,
    disposal_support TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT asset_diagnoses_condition_check CHECK (operational_condition IN (
        'OPERATIONAL', 'OPERATIONAL_WITH_RESTRICTIONS', 'IN_REPAIR', 'OUT_OF_SERVICE', 'OBSOLETE'
    )),
    CONSTRAINT asset_diagnoses_recommendation_check CHECK (recommendation IN (
        'KEEP', 'RECOVER', 'PENDING_EVALUATION', 'EVALUATE_DISPOSAL', 'RECOMMENDED_DISPOSAL'
    ))
);

CREATE INDEX IF NOT EXISTS idx_asset_diagnoses_asset_date
    ON asset_diagnoses (asset_id, diagnosed_on DESC, id DESC);

CREATE TABLE IF NOT EXISTS diagnosis_needs (
    id BIGSERIAL PRIMARY KEY,
    diagnosis_id BIGINT NOT NULL REFERENCES asset_diagnoses (id) ON DELETE CASCADE,
    description VARCHAR(255) NOT NULL,
    intervention_types VARCHAR(255) NOT NULL,
    priority VARCHAR(10) NOT NULL,
    safety_related BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    sort_order INTEGER NOT NULL DEFAULT 0,
    failure_report_id BIGINT REFERENCES failure_reports (id) ON DELETE SET NULL,
    preventive_maintenance_id BIGINT REFERENCES preventive_maintenances (id) ON DELETE SET NULL,
    CONSTRAINT diagnosis_needs_priority_check CHECK (priority IN ('ALTA', 'MEDIA', 'BAJA')),
    CONSTRAINT diagnosis_needs_status_check CHECK (status IN ('PENDING', 'IN_PROGRESS', 'DONE', 'DISCARDED'))
);

CREATE INDEX IF NOT EXISTS idx_diagnosis_needs_diagnosis
    ON diagnosis_needs (diagnosis_id);
