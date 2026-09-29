package com.agrosentinel.agrosentinel.dtos;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.agrosentinel.agrosentinel.models.MaintenanceType;

public record MaintenanceHistoryResponse(
        Long id,
        LocalDate maintenanceDate,
        MaintenanceType type,
        String tasksDescription,
        BigDecimal cost,
        String provider,
        LocalDate nextRecommendedDate) {
}