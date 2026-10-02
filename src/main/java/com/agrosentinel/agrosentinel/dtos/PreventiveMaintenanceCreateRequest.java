package com.agrosentinel.agrosentinel.dtos;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import com.agrosentinel.agrosentinel.models.MaintenanceAttentionIndex;

public record PreventiveMaintenanceCreateRequest(
        @NotNull @Positive Long assetId,
        @NotNull @Positive Long technicianId,
        @NotNull LocalDateTime scheduledAt,
        MaintenanceAttentionIndex attentionIndex,
        @Size(max = 5000) String predictiveContext) {

    public PreventiveMaintenanceCreateRequest(Long assetId, Long technicianId, LocalDateTime scheduledAt) {
        this(assetId, technicianId, scheduledAt, null, null);
    }
}
