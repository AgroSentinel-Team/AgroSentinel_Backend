package com.agrosentinel.agrosentinel.dtos;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotNull;

/**
 * Orden de trabajo a crear desde una necesidad del diagnóstico.
 * CORRECTIVE: reporte de falla asignado a un grupo (técnico opcional).
 * PREVENTIVE: mantenimiento programado con técnico y fecha.
 */
public record DiagnosisOrderRequest(
        @NotNull OrderType type,
        Long groupId,
        Long technicianId,
        LocalDateTime scheduledAt) {

    public enum OrderType {
        CORRECTIVE,
        PREVENTIVE
    }
}
