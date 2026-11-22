package com.agrosentinel.agrosentinel.dtos;

import java.util.List;

import com.agrosentinel.agrosentinel.models.SpecDataType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SpecFieldRequest(
        @Size(max = 60) String specKey,
        @NotBlank @Size(max = 120) String label,
        SpecDataType dataType,
        @Size(max = 20) String unit,
        List<String> options,
        Boolean required,
        @Size(max = 255) String helpText) {
}
