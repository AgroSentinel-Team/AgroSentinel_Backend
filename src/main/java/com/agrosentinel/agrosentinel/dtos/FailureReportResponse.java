package com.agrosentinel.agrosentinel.dtos;

import java.time.LocalDateTime;

import com.agrosentinel.agrosentinel.models.FailurePriority;
import com.agrosentinel.agrosentinel.models.FailureStatus;

public record FailureReportResponse(
        Long id,
        Long assetId,
        String assetName,
        Long userId,
        String userName,
        String tipoFalla,
        FailurePriority prioridad,
        String descripcion,
        LocalDateTime fechaReporte,
        FailureStatus estado,
        String evidenciaUrl,
        Long groupId,
        String groupName,
        Long assignedTechnicianId,
        String assignedTechnicianName,
        LocalDateTime fechaInicioRevision,
        String assetBrand,
        String assetModel,
        String assetSerialNumber,
        String assetInventoryCode) {
}
