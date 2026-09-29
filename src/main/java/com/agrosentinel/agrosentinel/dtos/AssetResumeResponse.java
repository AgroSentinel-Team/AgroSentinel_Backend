package com.agrosentinel.agrosentinel.dtos;

import java.util.List;

public record AssetResumeResponse(
        AssetResponse asset,
        List<MaintenanceHistoryResponse> maintenanceHistory) {
}