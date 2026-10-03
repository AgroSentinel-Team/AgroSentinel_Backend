package com.agrosentinel.agrosentinel.dtos;

import java.time.LocalDateTime;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PredictiveRecordRequest(
        @NotNull Long assetId,
        @NotBlank String machineId,
        LocalDateTime timestamp,
        @NotNull Double temperature,
        @NotNull Double vibration,
        @NotNull Double humidity,
        @NotNull Double pressure,
        @NotNull Double energyConsumption,
        @NotNull @Min(0) @Max(1) Integer machineStatus,
        @NotNull @Min(0) @Max(1) Integer predictionClass,
        String tipoFallaPredicha,
        @NotBlank String motorState,
        @NotNull @Min(0) @Max(1) Double confidence,
        @NotBlank String affectedComponents) {
}