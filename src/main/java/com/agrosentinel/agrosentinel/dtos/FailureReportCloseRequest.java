package com.agrosentinel.agrosentinel.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FailureReportCloseRequest(
        @NotBlank @Size(max = 5000) String reviewObservations) {
}
