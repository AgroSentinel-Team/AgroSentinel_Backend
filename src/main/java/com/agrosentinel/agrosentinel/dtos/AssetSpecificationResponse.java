package com.agrosentinel.agrosentinel.dtos;

import java.math.BigDecimal;

public record AssetSpecificationResponse(
        String specKey,
        String label,
        String value,
        BigDecimal numericValue,
        String unit,
        String displayValue,
        boolean custom) {
}
