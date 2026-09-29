package com.agrosentinel.agrosentinel.dtos;

public record PredictiveAssetResponse(
        Long assetId,
        String machineId,
        String name,
        String brand,
        String model,
        String location,
        PredictiveRecordResponse latestRecord) {
}