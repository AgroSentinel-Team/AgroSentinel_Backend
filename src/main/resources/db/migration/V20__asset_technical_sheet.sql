-- Ficha técnica de activos: datos base adicionales, especificaciones por tipo de equipo
-- y plantillas de campos por categoría.

-- 1. Datos base nuevos del activo.
ALTER TABLE assets
    ADD COLUMN IF NOT EXISTS equipment_type VARCHAR(120);

ALTER TABLE assets
    ADD COLUMN IF NOT EXISTS manufacture_year INTEGER;

-- Distingue "sin código porque falta asignarlo" de "el código existe pero no se suministró".
ALTER TABLE assets
    ADD COLUMN IF NOT EXISTS inventory_code_status VARCHAR(20) NOT NULL DEFAULT 'ASSIGNED';

UPDATE assets
SET inventory_code_status = 'PENDING'
WHERE inventory_code IS NULL OR btrim(inventory_code) = '';

ALTER TABLE assets
    DROP CONSTRAINT IF EXISTS assets_inventory_code_status_check;

ALTER TABLE assets
    ADD CONSTRAINT assets_inventory_code_status_check
        CHECK (inventory_code_status IN ('ASSIGNED', 'PENDING', 'NOT_PROVIDED'));

-- 2. Nuevo estado "fuera de servicio" (distinto de "en reparación").
ALTER TABLE assets
    DROP CONSTRAINT IF EXISTS assets_operational_status_check;

ALTER TABLE assets
    ADD CONSTRAINT assets_operational_status_check
        CHECK (operational_status IN (
            'OPERATIONAL',
            'OPERATIONAL_WITH_RESTRICTIONS',
            'IN_REPAIR',
            'OUT_OF_SERVICE',
            'OBSOLETE'
        ));

-- 3. El serial deja de ser único: los inventarios reales traen seriales repetidos por error
--    de digitación y deben poder registrarse para corregirlos después. La aplicación advierte.
DO $$
DECLARE
    constraint_row RECORD;
BEGIN
    FOR constraint_row IN
        SELECT con.conname
        FROM pg_constraint con
        JOIN pg_attribute att
            ON att.attrelid = con.conrelid AND att.attnum = ANY (con.conkey)
        WHERE con.conrelid = 'assets'::regclass
          AND con.contype = 'u'
          AND att.attname = 'serial_number'
    LOOP
        EXECUTE format('ALTER TABLE assets DROP CONSTRAINT %I', constraint_row.conname);
    END LOOP;
END $$;

CREATE INDEX IF NOT EXISTS idx_assets_serial_number_lower
    ON assets (lower(serial_number));

CREATE INDEX IF NOT EXISTS idx_assets_inventory_code_lower
    ON assets (lower(inventory_code));

-- 4. Plantilla de especificaciones por categoría.
CREATE TABLE IF NOT EXISTS category_spec_templates (
    id BIGSERIAL PRIMARY KEY,
    category_id BIGINT NOT NULL REFERENCES categories (id) ON DELETE CASCADE,
    spec_key VARCHAR(60) NOT NULL,
    label VARCHAR(120) NOT NULL,
    data_type VARCHAR(20) NOT NULL DEFAULT 'TEXT',
    unit VARCHAR(20),
    options TEXT,
    required BOOLEAN NOT NULL DEFAULT FALSE,
    sort_order INTEGER NOT NULL DEFAULT 0,
    help_text VARCHAR(255),
    CONSTRAINT uk_category_spec_templates_key UNIQUE (category_id, spec_key),
    CONSTRAINT category_spec_templates_data_type_check
        CHECK (data_type IN ('TEXT', 'NUMBER', 'OPTION'))
);

-- 5. Especificaciones registradas para cada activo.
CREATE TABLE IF NOT EXISTS asset_specifications (
    id BIGSERIAL PRIMARY KEY,
    asset_id BIGINT NOT NULL REFERENCES assets (id) ON DELETE CASCADE,
    spec_key VARCHAR(60) NOT NULL,
    label VARCHAR(120) NOT NULL,
    value_text VARCHAR(500),
    value_number NUMERIC(14, 3),
    unit VARCHAR(20),
    sort_order INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT uk_asset_specifications_key UNIQUE (asset_id, spec_key)
);

CREATE INDEX IF NOT EXISTS idx_asset_specifications_asset
    ON asset_specifications (asset_id);
