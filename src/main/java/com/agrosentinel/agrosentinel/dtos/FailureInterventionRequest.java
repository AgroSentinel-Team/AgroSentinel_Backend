package com.agrosentinel.agrosentinel.dtos;

import java.time.LocalDateTime;
import java.util.List;

import com.agrosentinel.agrosentinel.models.EquipmentFinalState;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record FailureInterventionRequest(
        @NotBlank @Size(max = 1000) String equipmentIntervened,
        @NotBlank @Size(max = 5000) String initialReportedSymptom,
        @NotNull LocalDateTime reportedAt,
        @NotNull LocalDateTime inspectionStartedAt,
        @NotBlank @Size(max = 5000) String actualFailure,
        @NotBlank @Size(max = 5000) String rootCause,
        @NotBlank @Size(max = 10000) String tasksPerformed,
        @NotNull @Size(max = 100) List<@Valid MaintenancePartRequest> replacedParts,
        @NotNull @Min(0) Integer workMinutes,
        @NotBlank @Size(max = 10000) String testsPerformed,
        @NotNull EquipmentFinalState finalEquipmentState,
        @NotBlank @Size(max = 5000) String recommendations,
        @Size(max = 5000) String pendingTasks,
        @NotBlank @Size(max = 160) String technicianName,
        @Size(max = 160) String receivedBy) {
}
