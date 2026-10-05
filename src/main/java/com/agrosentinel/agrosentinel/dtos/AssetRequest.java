package com.agrosentinel.agrosentinel.dtos;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.agrosentinel.agrosentinel.models.OperationalStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AssetRequest(
        @NotBlank String name,
        String brand,
        String model,
        String serialNumber,
        @Size(max = 80) String inventoryCode,
        String extraSpecifications,
        @NotNull Long categoryId,
        @NotNull Long responsibleId,
        LocalDate acquisitionDate,
        BigDecimal acquisitionCost,
        String provider,
        LocalDate warrantyExpirationDate,
        String location,
        OperationalStatus operationalStatus,
        Boolean isActive,
        Boolean hasMotor) {
}