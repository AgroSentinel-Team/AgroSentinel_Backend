-- Registro de operaciones enviadas desde dispositivos (cola offline).
-- Permite reintentar una operación sin duplicarla: si la misma clave llega dos veces,
-- se devuelve la respuesta guardada en lugar de volver a ejecutarla.
CREATE TABLE IF NOT EXISTS client_operations (
    id BIGSERIAL PRIMARY KEY,
    idempotency_key VARCHAR(100) NOT NULL,
    user_email VARCHAR(255) NOT NULL,
    http_method VARCHAR(10) NOT NULL,
    request_path VARCHAR(500) NOT NULL,
    status VARCHAR(20) NOT NULL,
    response_status INTEGER,
    response_content_type VARCHAR(150),
    response_body TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP,
    CONSTRAINT uk_client_operations_user_key UNIQUE (user_email, idempotency_key)
);

CREATE INDEX IF NOT EXISTS idx_client_operations_created
    ON client_operations (created_at);
