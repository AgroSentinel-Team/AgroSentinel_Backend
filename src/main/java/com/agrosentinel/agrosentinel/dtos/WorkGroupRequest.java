package com.agrosentinel.agrosentinel.dtos;

import java.util.Set;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record WorkGroupRequest(
        @NotBlank @Size(max = 120) String name,
        @Size(max = 2000) String description,
        @NotNull Long leaderId,
        @NotNull Set<@NotNull Long> memberIds,
        @NotNull Set<@NotBlank String> grantedRoles) {
}
