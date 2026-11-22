package com.agrosentinel.agrosentinel.dtos;

import java.util.List;

import com.agrosentinel.agrosentinel.models.InterventionType;
import com.agrosentinel.agrosentinel.models.NeedPriority;
import com.agrosentinel.agrosentinel.models.NeedStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** id presente = necesidad existente (conserva su orden de trabajo vinculada). */
public record DiagnosisNeedRequest(
        Long id,
        @NotBlank @Size(max = 255) String description,
        @NotEmpty @Size(max = 8) List<@NotNull InterventionType> interventionTypes,
        @NotNull NeedPriority priority,
        Boolean safetyRelated,
        NeedStatus status) {
}
