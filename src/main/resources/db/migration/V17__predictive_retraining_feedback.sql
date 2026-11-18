ALTER TABLE predictive_records
    ADD COLUMN IF NOT EXISTS guardar_para_reentrenamiento BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE predictive_records
SET guardar_para_reentrenamiento = TRUE
WHERE falla_real_confirmada IS NOT NULL
  AND BTRIM(falla_real_confirmada) <> '';
