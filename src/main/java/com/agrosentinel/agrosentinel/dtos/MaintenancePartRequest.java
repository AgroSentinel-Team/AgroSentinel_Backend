package com.agrosentinel.agrosentinel.dtos;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record MaintenancePartRequest(
        @NotBlank @Size(max = 120) String partNumber,
        @NotBlank @Size(max = 160) String name,
        @Size(max = 120) String brand,
        @NotNull @DecimalMin(value = "0.001") BigDecimal quantity,
        Long itemId,
        Long sourceGroupId,
        String sourceZoneName) {

    public MaintenancePartRequest(String partNumber, String name, String brand, BigDecimal quantity) {
        this(partNumber, name, brand, quantity, null, null, null);
    }
}
