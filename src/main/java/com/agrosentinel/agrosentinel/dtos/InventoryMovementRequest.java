package com.agrosentinel.agrosentinel.dtos;

import java.math.BigDecimal;

import com.agrosentinel.agrosentinel.models.InventoryMovementType;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record InventoryMovementRequest(
        @NotNull InventoryMovementType tipo,
        @NotNull @DecimalMin(value = "0", inclusive = false) @Digits(integer = 11, fraction = 3) BigDecimal cantidad,
        @Size(max = 2000) String motivo) {
}
