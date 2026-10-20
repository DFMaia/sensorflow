package com.vanguard.sensorservice.repository;

import com.vanguard.sensorservice.model.Sensor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Usa o MySQL de verdade (sensordb). Cada teste roda numa transação que é desfeita
 * (rollback) no final, então nada fica gravado no banco.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class SensorRepositoryTest {

    @Autowired
    private SensorRepository sensorRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void save_shouldGenerateIdAndCreatedAt() {
        Sensor saved = sensorRepository.save(new Sensor("Câmara de teste", 5.0));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    void findById_shouldReadSensorFromDatabase() {
        Sensor saved = sensorRepository.saveAndFlush(new Sensor("Câmara de teste", 5.0));
        entityManager.clear();

        Optional<Sensor> found = sensorRepository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Câmara de teste");
        assertThat(found.get().getMaxTemperature()).isEqualTo(5.0);
        assertThat(found.get().getCreatedAt()).isNotNull();
    }

    @Test
    void findById_shouldReturnEmptyWhenSensorDoesNotExist() {
        Optional<Sensor> found = sensorRepository.findById(-1L);

        assertThat(found).isEmpty();
    }
}
