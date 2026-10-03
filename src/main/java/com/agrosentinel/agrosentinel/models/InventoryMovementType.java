package com.agrosentinel.agrosentinel.models;

import java.util.Locale;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum InventoryMovementType {
    ENTRADA,
    SALIDA;

    @JsonCreator
    public static InventoryMovementType fromValue(String value) {
        if (value == null) {
            throw new IllegalArgumentException("El tipo de movimiento es obligatorio");
        }
        return valueOf(value.trim().toUpperCase(Locale.ROOT));
    }

    @JsonValue
    public String toValue() {
        return name().toLowerCase(Locale.ROOT);
    }
}
