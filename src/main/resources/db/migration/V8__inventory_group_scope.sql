ALTER TABLE inventory_items
    ADD COLUMN IF NOT EXISTS group_id BIGINT REFERENCES work_groups(id);

CREATE INDEX IF NOT EXISTS idx_inventory_items_group_id
    ON inventory_items (group_id);
