ALTER TABLE predictive_records
    ADD COLUMN IF NOT EXISTS tendencia_lstm VARCHAR(120),
    ADD COLUMN IF NOT EXISTS confianza_lstm DOUBLE PRECISION;
