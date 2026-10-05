package com.agrosentinel.agrosentinel.dtos;

import java.math.BigDecimal;

public record MaintenancePartResponse(
        String partNumber,
        String name,
        String brand,
        BigDecimal quantity) {
}
