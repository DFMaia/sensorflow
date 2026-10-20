package com.vanguard.sensorservice.service;

import com.vanguard.sensorservice.dto.SensorRequest;
import com.vanguard.sensorservice.dto.SensorResponse;
import com.vanguard.sensorservice.exception.SensorNotFoundException;
import com.vanguard.sensorservice.model.Sensor;
import com.vanguard.sensorservice.repository.SensorRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SensorServiceTest {

    @Mock
    private SensorRepository sensorRepository;

    @InjectMocks
    private SensorService sensorService;

    @Test
    void create_shouldSaveSensorAndReturnResponse() {
        SensorRequest request = new SensorRequest("Câmara de carnes", 4.0);
        when(sensorRepository.save(any(Sensor.class))).thenReturn(sensorWithId(1L, "Câmara de carnes", 4.0));

        SensorResponse response = sensorService.create(request);

        ArgumentCaptor<Sensor> captor = ArgumentCaptor.forClass(Sensor.class);
        verify(sensorRepository).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("Câmara de carnes");
        assertThat(captor.getValue().getMaxTemperature()).isEqualTo(4.0);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Câmara de carnes");
        assertThat(response.maxTemperature()).isEqualTo(4.0);
    }

    @Test
    void findAll_shouldReturnAllSensors() {
        when(sensorRepository.findAll()).thenReturn(List.of(
                sensorWithId(1L, "Câmara de carnes", 4.0),
                sensorWithId(2L, "Câmara de laticínios", 7.0)
        ));

        List<SensorResponse> responses = sensorService.findAll();

        assertThat(responses).hasSize(2);
        assertThat(responses)
                .extracting(SensorResponse::name)
                .containsExactly("Câmara de carnes", "Câmara de laticínios");
    }

    @Test
    void findAll_shouldReturnEmptyListWhenThereAreNoSensors() {
        when(sensorRepository.findAll()).thenReturn(List.of());

        List<SensorResponse> responses = sensorService.findAll();

        assertThat(responses).isEmpty();
    }

    @Test
    void findById_shouldReturnSensorWhenItExists() {
        when(sensorRepository.findById(1L)).thenReturn(Optional.of(sensorWithId(1L, "Câmara de carnes", 4.0)));

        SensorResponse response = sensorService.findById(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Câmara de carnes");
    }

    @Test
    void findById_shouldThrowExceptionWhenSensorDoesNotExist() {
        when(sensorRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sensorService.findById(99L))
                .isInstanceOf(SensorNotFoundException.class)
                .hasMessageContaining("99");
    }

    private Sensor sensorWithId(Long id, String name, Double maxTemperature) {
        Sensor sensor = new Sensor(name, maxTemperature);
        ReflectionTestUtils.setField(sensor, "id", id);
        return sensor;
    }
}
