package com.agrosentinel.agrosentinel.dtos;

import java.time.LocalDate;
import java.util.List;
import java.math.BigDecimal;

import com.agrosentinel.agrosentinel.models.MaintenanceOutcome;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PreventiveMaintenanceExecutionRequest(
        @NotEmpty @Size(max = 100) List<@Valid PreventiveMaintenanceTaskRequest> tasks,
        @Size(max = 5000) String replacedParts,
        @NotBlank @Size(max = 10000) String workDescription,
        @Size(max = 10000) String technicianNotes,
        @NotNull MaintenanceOutcome outcome,
        LocalDate nextMaintenanceDate,
        boolean confirmed,
        @DecimalMin("0.0") @Digits(integer = 10, fraction = 2) BigDecimal horometerReading) {

    public PreventiveMaintenanceExecutionRequest(
            List<@Valid PreventiveMaintenanceTaskRequest> tasks,
            String replacedParts,
            String workDescription,
            String technicianNotes,
            MaintenanceOutcome outcome,
            LocalDate nextMaintenanceDate,
            boolean confirmed) {
        this(tasks, replacedParts, workDescription, technicianNotes, outcome, nextMaintenanceDate, confirmed, null);
    }
}
