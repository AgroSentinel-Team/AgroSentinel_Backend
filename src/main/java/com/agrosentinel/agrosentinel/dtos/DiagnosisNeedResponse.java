package com.agrosentinel.agrosentinel.dtos;

import java.util.List;

import com.agrosentinel.agrosentinel.models.InterventionType;
import com.agrosentinel.agrosentinel.models.NeedPriority;
import com.agrosentinel.agrosentinel.models.NeedStatus;

public record DiagnosisNeedResponse(
        Long id,
        String description,
        List<InterventionType> interventionTypes,
        NeedPriority priority,
        boolean safetyRelated,
        NeedStatus status,
        Long failureReportId,
        Long preventiveMaintenanceId,
        String orderLabel,
        String orderStatus) {
}
