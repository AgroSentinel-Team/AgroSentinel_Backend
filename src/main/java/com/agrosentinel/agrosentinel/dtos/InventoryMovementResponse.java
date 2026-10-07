package com.agrosentinel.agrosentinel.dtos;

import java.math.BigDecimal;
import java.time.Instant;

import com.agrosentinel.agrosentinel.models.InventoryMovementType;

public record InventoryMovementResponse(
        Long id,
        Long itemId,
        String itemName,
        String itemUnit,
        InventoryMovementType tipo,
        BigDecimal cantidad,
        String responsable,
        String rol,
        String motivo,
        Instant fecha,
        BigDecimal stockActual,
        Long groupId,
        String groupName) {
}
