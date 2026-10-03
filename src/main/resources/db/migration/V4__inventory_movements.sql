CREATE TABLE IF NOT EXISTS inventory_items (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(160) NOT NULL,
    stock NUMERIC(14, 3) NOT NULL DEFAULT 0,
    CONSTRAINT inventory_items_stock_nonnegative CHECK (stock >= 0)
);

CREATE TABLE IF NOT EXISTS inventory_movements (
    id BIGSERIAL PRIMARY KEY,
    item_id BIGINT NOT NULL REFERENCES inventory_items(id),
    tipo VARCHAR(10) NOT NULL CHECK (tipo IN ('ENTRADA', 'SALIDA')),
    cantidad NUMERIC(14, 3) NOT NULL CHECK (cantidad > 0),
    responsable VARCHAR(160) NOT NULL,
    rol VARCHAR(40) NOT NULL,
    motivo TEXT,
    fecha TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_inventory_movements_fecha
    ON inventory_movements (fecha DESC, id DESC);

CREATE INDEX IF NOT EXISTS idx_inventory_movements_item_id
    ON inventory_movements (item_id);
