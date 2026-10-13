package com.vanguard.sensorservice.repository;

import com.vanguard.sensorservice.model.Sensor;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SensorRepository extends JpaRepository<Sensor, Long> {
}
