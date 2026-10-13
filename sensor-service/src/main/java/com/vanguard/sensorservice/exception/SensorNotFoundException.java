package com.vanguard.sensorservice.exception;

public class SensorNotFoundException  extends RuntimeException {

    public SensorNotFoundException(Long id) {
        super("Sensor not found: id=" + id);
    }

}
