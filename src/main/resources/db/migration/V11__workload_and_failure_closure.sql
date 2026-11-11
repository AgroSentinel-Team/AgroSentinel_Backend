ALTER TABLE failure_reports
    ADD COLUMN IF NOT EXISTS fecha_asignacion TIMESTAMP WITHOUT TIME ZONE,
    ADD COLUMN IF NOT EXISTS fecha_cierre TIMESTAMP WITHOUT TIME ZONE;

UPDATE failure_reports
SET fecha_asignacion = fecha_reporte
WHERE fecha_asignacion IS NULL
  AND assigned_technician_id IS NOT NULL;

ALTER TABLE failure_reports
    DROP CONSTRAINT IF EXISTS failure_reports_estado_check;

ALTER TABLE failure_reports
    ADD CONSTRAINT failure_reports_estado_check
        CHECK (estado IN (
            'PENDIENTE',
            'ASIGNADA',
            'EN_EJECUCION',
            'EN_CIERRE',
            'EN_REVISION',
            'RESUELTO',
            'ESPERA_REPUESTO'
        ));

UPDATE failure_reports
SET estado = 'ASIGNADA'
WHERE estado = 'PENDIENTE'
  AND assigned_technician_id IS NOT NULL;

UPDATE failure_reports
SET estado = 'EN_EJECUCION'
WHERE estado = 'EN_REVISION';

UPDATE failure_reports report
SET estado = 'EN_CIERRE'
WHERE report.estado = 'ESPERA_REPUESTO'
  AND EXISTS (
      SELECT 1 FROM maintenance_history history
      WHERE history.failure_report_id = report.id
  );

UPDATE failure_reports report
SET fecha_cierre = history.executed_at
FROM maintenance_history history
WHERE report.id = history.failure_report_id
  AND report.estado = 'RESUELTO'
  AND report.fecha_cierre IS NULL;

ALTER TABLE maintenance_history
    ADD COLUMN IF NOT EXISTS review_observations TEXT,
    ADD COLUMN IF NOT EXISTS reviewed_by VARCHAR(160),
    ADD COLUMN IF NOT EXISTS reviewed_at TIMESTAMP WITHOUT TIME ZONE;
