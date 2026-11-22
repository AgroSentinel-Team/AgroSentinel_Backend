package com.agrosentinel.agrosentinel.dtos;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.agrosentinel.agrosentinel.models.InventoryCodeStatus;
import com.agrosentinel.agrosentinel.models.OperationalStatus;

public record AssetResponse(
        Long id,
        String name,
        String brand,
        String model,
        String serialNumber,
        String inventoryCode,
        String extraSpecifications,
        Long categoryId,
        String categoryName,
        Long responsibleId,
        String responsibleName,
        LocalDate acquisitionDate,
        BigDecimal acquisitionCost,
        String provider,
        LocalDate warrantyExpirationDate,
        String location,
        OperationalStatus operationalStatus,
        boolean isActive,
        boolean hasMotor,
        String predictiveId,
        String imageUrl,
        BigDecimal currentHorometer,
        String equipmentType,
        Integer manufactureYear,
        InventoryCodeStatus inventoryCodeStatus,
        List<AssetSpecificationResponse> specifications,
        int completeness,
        List<String> missingFields) {
}