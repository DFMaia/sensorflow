package com.vanguard.sensorservice.controller;

import com.vanguard.sensorservice.dto.SensorRequest;
import com.vanguard.sensorservice.dto.SensorResponse;
import com.vanguard.sensorservice.service.SensorService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/sensors")
public class SensorController {

    private final SensorService sensorService;

    public SensorController(SensorService sensorService) {
        this.sensorService = sensorService;
    }

    @PostMapping
    public ResponseEntity<SensorResponse> create(@Valid @RequestBody SensorRequest request) {
        SensorResponse response = sensorService.create(request);
        URI location = URI.create("/sensors/" + response.id());
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    public List<SensorResponse> findAll() {
        return sensorService.findAll();
    }

    @GetMapping("/{id}")
    public SensorResponse findById(@PathVariable Long id) {
        return sensorService.findById(id);
    }
}
