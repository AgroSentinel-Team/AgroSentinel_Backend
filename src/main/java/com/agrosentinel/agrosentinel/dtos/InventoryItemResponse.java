package com.agrosentinel.agrosentinel.dtos;

import java.math.BigDecimal;

public record InventoryItemResponse(
        Long id,
        String item,
        String category,
        BigDecimal stock,
        String unit,
        BigDecimal minimum) {
}
