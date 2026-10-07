package com.agrosentinel.agrosentinel.dtos;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record InventoryInitialEntryRequest(
        @NotBlank @Size(max = 160) String item,
        @NotBlank @Size(max = 80) String category,
        @NotBlank @Size(max = 30) String unit,
        @NotNull @DecimalMin("0") @Digits(integer = 11, fraction = 3) BigDecimal minimum,
        @NotNull @DecimalMin(value = "0", inclusive = false) @Digits(integer = 11, fraction = 3) BigDecimal cantidad,
        @Size(max = 2000) String motivo,
        Long groupId) {
}
