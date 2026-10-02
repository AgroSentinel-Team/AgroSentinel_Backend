package com.agrosentinel.agrosentinel.dtos;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.agrosentinel.agrosentinel.models.MaintenanceAttentionIndex;
import com.agrosentinel.agrosentinel.models.PreventiveMaintenanceStatus;

public record PreventiveMaintenanceResponse(
        Long id,
        Long assetId,
        String assetName,
        String categoryName,
        int maintenanceFrequencyDays,
        Long technicianId,
        String technicianName,
        LocalDateTime scheduledAt,
        PreventiveMaintenanceStatus status,
        LocalDateTime executedAt,
        String replacedParts,
        String workDescription,
        String technicianNotes,
        LocalDate nextMaintenanceDate,
        LocalDateTime confirmedAt,
        List<PreventiveMaintenanceTaskResponse> tasks,
        List<String> evidences,
        MaintenanceAttentionIndex attentionIndex,
        String predictiveContext) {
}
