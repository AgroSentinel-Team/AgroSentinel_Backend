package com.agrosentinel.agrosentinel.dtos;

import java.time.LocalDate;
import java.util.List;

import com.agrosentinel.agrosentinel.models.DiagnosisRecommendation;
import com.agrosentinel.agrosentinel.models.OperationalStatus;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AssetDiagnosisRequest(
        @NotNull Long assetId,
        LocalDate diagnosedOn,
        @Size(max = 5000) String currentStateDescription,
        @Size(max = 5000) String technicalDiagnosis,
        @Size(max = 5000) String technicalConcept,
        @Size(max = 5000) String conclusion,
        @NotNull OperationalStatus operationalCondition,
        @NotNull DiagnosisRecommendation recommendation,
        @Size(max = 5000) String recommendationDetail,
        @Size(max = 5000) String disposalSupport,
        // Si es true (por defecto) y es el diagnóstico más reciente, actualiza el estado del activo.
        Boolean updateAssetStatus,
        @Size(max = 30) List<@Valid DiagnosisNeedRequest> needs) {
}
