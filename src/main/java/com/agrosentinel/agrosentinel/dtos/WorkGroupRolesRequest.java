package com.agrosentinel.agrosentinel.dtos;

import java.util.Set;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;

public record WorkGroupRolesRequest(@NotNull Set<@NotBlank String> roles) {
}
