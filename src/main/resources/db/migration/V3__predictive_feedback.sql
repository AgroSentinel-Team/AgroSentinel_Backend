ALTER TABLE predictive_records
    ADD COLUMN IF NOT EXISTS tipo_falla_predicha VARCHAR(120),
    ADD COLUMN IF NOT EXISTS falla_real_confirmada VARCHAR(120);
