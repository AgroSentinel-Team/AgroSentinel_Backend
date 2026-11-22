package com.agrosentinel.agrosentinel.dtos;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.agrosentinel.agrosentinel.models.DiagnosisRecommendation;
import com.agrosentinel.agrosentinel.models.OperationalStatus;

public record AssetDiagnosisResponse(
        Long id,
        Long assetId,
        String assetName,
        LocalDate diagnosedOn,
        String inspectorName,
        String currentStateDescription,
        String technicalDiagnosis,
        String technicalConcept,
        String conclusion,
        OperationalStatus operationalCondition,
        DiagnosisRecommendation recommendation,
        String classificationLabel,
        String recommendationDetail,
        String disposalSupport,
        List<DiagnosisNeedResponse> needs,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
