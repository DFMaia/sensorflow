package com.vanguard.sensorservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SensorRequest(
        @NotBlank @Size(max = 100) String name,
        @NotNull Double maxTemperature
) {
}
