CREATE TABLE IF NOT EXISTS inventory_tasks (
    id BIGSERIAL PRIMARY KEY,
    description VARCHAR(240) NOT NULL,
    completed BOOLEAN NOT NULL DEFAULT FALSE,
    created_by VARCHAR(160) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_inventory_tasks_status_created_at
    ON inventory_tasks (completed, created_at DESC);
