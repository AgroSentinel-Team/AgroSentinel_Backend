package com.agrosentinel.agrosentinel.dtos;

import com.agrosentinel.agrosentinel.models.OperationalStatus;

public record FailureAssetOption(
        Long id,
        String name,
        String brand,
        String model,
        String serialNumber,
        String location,
        Long responsibleId,
        String responsibleName,
        OperationalStatus operationalStatus,
        boolean assignedToCurrentUser) {
}
