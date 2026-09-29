package com.agrosentinel.agrosentinel.dtos;

import java.time.LocalDateTime;

public record PredictiveRecordResponse(
        Long id,
        Long assetId,
        String machineId,
        LocalDateTime timestamp,
        double temperature,
        double vibration,
        double humidity,
        double pressure,
        double energyConsumption,
        int machineStatus,
        int predictionClass,
        String motorState,
        double confidence,
        String affectedComponents) {
}