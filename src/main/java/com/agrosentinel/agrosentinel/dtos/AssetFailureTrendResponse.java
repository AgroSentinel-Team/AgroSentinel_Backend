package com.agrosentinel.agrosentinel.dtos;

import java.math.BigDecimal;
import java.util.List;

public record AssetFailureTrendResponse(
        Long assetId,
        long failureCount,
        Double averageDaysBetweenFailures,
        int baseIntervalDays,
        int suggestedIntervalDays,
        BigDecimal currentHorometer,
        List<String> recurringFailures,
        String summary) {
}
