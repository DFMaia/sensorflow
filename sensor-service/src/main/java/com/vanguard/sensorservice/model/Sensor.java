package com.vanguard.sensorservice.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "sensors")
public class Sensor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "max_temperature", nullable = false)
    private Double maxTemperature;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Sensor() {
    }

    public Sensor(String name, Double maxTemperature) {
        this.name = name;
        this.maxTemperature = maxTemperature;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Double getMaxTemperature() {
        return maxTemperature;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

}
