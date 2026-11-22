package com.agrosentinel.agrosentinel.dtos;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SpecTemplateRequest(@NotNull @Size(max = 40) List<@Valid SpecFieldRequest> fields) {
}
