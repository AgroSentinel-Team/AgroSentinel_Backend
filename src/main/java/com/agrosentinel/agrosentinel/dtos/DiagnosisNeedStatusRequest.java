package com.agrosentinel.agrosentinel.dtos;

import com.agrosentinel.agrosentinel.models.NeedStatus;

import jakarta.validation.constraints.NotNull;

public record DiagnosisNeedStatusRequest(@NotNull NeedStatus status) {
}
