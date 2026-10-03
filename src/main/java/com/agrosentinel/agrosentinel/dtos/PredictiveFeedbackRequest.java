package com.agrosentinel.agrosentinel.dtos;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PredictiveFeedbackRequest(
        @JsonProperty("falla_real_confirmada")
        @JsonAlias("fallaRealConfirmada")
        @NotBlank @Size(max = 120) String fallaRealConfirmada,
        @JsonProperty("machine_id")
        @JsonAlias("machineId")
        String machineId,
        LocalDateTime timestamp) {
}
