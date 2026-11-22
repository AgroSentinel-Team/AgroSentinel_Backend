package com.agrosentinel.agrosentinel.dtos;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.math.BigDecimal;

import com.agrosentinel.agrosentinel.models.EquipmentFinalState;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
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
        @Size(max = 160) String receivedBy,
        @DecimalMin("0.0") @Digits(integer = 10, fraction = 2) BigDecimal horometerReading,
        /** Hora real en que el técnico terminó (puede llegar tarde si estaba sin internet). */
        OffsetDateTime completedAt) {

    public FailureInterventionRequest(
            String equipmentIntervened,
            String initialReportedSymptom,
            LocalDateTime reportedAt,
            LocalDateTime inspectionStartedAt,
            String actualFailure,
            String rootCause,
            String tasksPerformed,
            List<@Valid MaintenancePartRequest> replacedParts,
            Integer workMinutes,
            String testsPerformed,
            EquipmentFinalState finalEquipmentState,
            String recommendations,
            String pendingTasks,
            String technicianName,
            String receivedBy,
            BigDecimal horometerReading) {
        this(equipmentIntervened, initialReportedSymptom, reportedAt, inspectionStartedAt, actualFailure, rootCause,
                tasksPerformed, replacedParts, workMinutes, testsPerformed, finalEquipmentState, recommendations,
                pendingTasks, technicianName, receivedBy, horometerReading, null);
    }

    public FailureInterventionRequest(
            String equipmentIntervened,
            String initialReportedSymptom,
            LocalDateTime reportedAt,
            LocalDateTime inspectionStartedAt,
            String actualFailure,
            String rootCause,
            String tasksPerformed,
            List<@Valid MaintenancePartRequest> replacedParts,
            Integer workMinutes,
            String testsPerformed,
            EquipmentFinalState finalEquipmentState,
            String recommendations,
            String pendingTasks,
            String technicianName,
            String receivedBy) {
        this(equipmentIntervened, initialReportedSymptom, reportedAt, inspectionStartedAt, actualFailure, rootCause,
                tasksPerformed, replacedParts, workMinutes, testsPerformed, finalEquipmentState, recommendations,
                pendingTasks, technicianName, receivedBy, null, null);
    }
}
