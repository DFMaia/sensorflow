package com.vanguard.sensorservice.service;

import com.vanguard.sensorservice.dto.SensorRequest;
import com.vanguard.sensorservice.dto.SensorResponse;
import com.vanguard.sensorservice.exception.SensorNotFoundException;
import com.vanguard.sensorservice.model.Sensor;
import com.vanguard.sensorservice.repository.SensorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SensorService {

    private final SensorRepository sensorRepository;

    public SensorService(SensorRepository sensorRepository) {
        this.sensorRepository = sensorRepository;
    }

    @Transactional
    public SensorResponse create(SensorRequest request) {
        Sensor sensor = new Sensor(request.name(), request.maxTemperature());
        Sensor saved = sensorRepository.save(sensor);
        return SensorResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<SensorResponse> findAll() {
        return sensorRepository.findAll()
                .stream()
                .map(SensorResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public SensorResponse findById(Long id) {
        return sensorRepository.findById(id)
                .map(SensorResponse::from)
                .orElseThrow(() -> new SensorNotFoundException(id));
    }

}
