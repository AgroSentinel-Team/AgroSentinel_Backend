-- Configuración institucional de los informes impresos (logos, textos, firma).
-- Una sola fila (id = 1) con el contenido en JSON.
CREATE TABLE IF NOT EXISTS report_settings (
    id SMALLINT PRIMARY KEY,
    settings_json TEXT NOT NULL,
    updated_by VARCHAR(255),
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT report_settings_single_row CHECK (id = 1)
);
