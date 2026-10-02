package com.agrosentinel.agrosentinel.dtos;

import java.time.LocalDate;
import java.util.List;

import com.agrosentinel.agrosentinel.models.MaintenanceOutcome;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
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
        boolean confirmed) {
}
