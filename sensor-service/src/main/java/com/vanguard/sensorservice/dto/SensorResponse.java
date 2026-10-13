package com.vanguard.sensorservice.dto;

import com.vanguard.sensorservice.model.Sensor;

import java.time.Instant;

public record SensorResponse(
        Long id,
        String name,
        Double maxTemperature,
        Instant createdAt
) {

    public static SensorResponse from(Sensor sensor) {
    return new SensorResponse(
            sensor.getId(),
            sensor.getName(),
            sensor.getMaxTemperature(),
            sensor.getCreatedAt()
        );
    }
}