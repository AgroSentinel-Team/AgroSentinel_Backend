package com.agrosentinel.agrosentinel.dtos;

import java.math.BigDecimal;
import com.fasterxml.jackson.annotation.JsonProperty;

public record InventoryItemResponse(
        Long id,
        String item,
        String category,
        BigDecimal stock,
        String unit,
        BigDecimal minimum,
        Long groupId,
        String groupName) {

    @JsonProperty("name")
    public String name() {
        return item;
    }
}
