package com.agrosentinel.agrosentinel.dtos;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.agrosentinel.agrosentinel.models.EquipmentFinalState;
import com.agrosentinel.agrosentinel.models.MaintenanceHistoryStatus;
import com.agrosentinel.agrosentinel.models.MaintenanceType;

public record MaintenanceHistoryResponse(
        Long id,
        LocalDate maintenanceDate,
        MaintenanceType type,
        String tasksDescription,
        BigDecimal cost,
        String provider,
        LocalDate nextRecommendedDate,
        MaintenanceHistoryStatus status,
        LocalDateTime scheduledAt,
        LocalDateTime executedAt,
        String technicianName,
        String replacedParts,
        LocalDateTime signedAt,
        List<String> evidences,
        Long failureReportId,
        String equipmentIntervened,
        String initialReportedSymptom,
        LocalDateTime interventionReportedAt,
        LocalDateTime inspectionStartedAt,
        String actualFailure,
        String rootCause,
        Integer workMinutes,
        List<MaintenancePartResponse> parts,
        String testsPerformed,
        EquipmentFinalState finalEquipmentState,
        String recommendations,
        String pendingTasks,
        String receivedBy,
        Long assetId,
        String assetName) {
}