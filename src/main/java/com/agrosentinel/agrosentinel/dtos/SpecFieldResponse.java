package com.agrosentinel.agrosentinel.dtos;

import java.util.List;

import com.agrosentinel.agrosentinel.models.SpecDataType;

public record SpecFieldResponse(
        String specKey,
        String label,
        SpecDataType dataType,
        String unit,
        List<String> options,
        boolean required,
        int sortOrder,
        String helpText) {
}
