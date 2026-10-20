package com.vanguard.sensorservice.controller;

import com.vanguard.sensorservice.dto.SensorRequest;
import com.vanguard.sensorservice.dto.SensorResponse;
import com.vanguard.sensorservice.exception.SensorNotFoundException;
import com.vanguard.sensorservice.service.SensorService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SensorController.class)
class SensorControllerTest {

    private static final Instant CREATED_AT = Instant.parse("2026-10-13T12:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SensorService sensorService;

    @Test
    void create_shouldReturn201WithLocationAndBody() throws Exception {
        when(sensorService.create(any(SensorRequest.class)))
                .thenReturn(new SensorResponse(1L, "Câmara de carnes", 4.0, CREATED_AT));

        mockMvc.perform(post("/sensors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "name": "Câmara de carnes", "maxTemperature": 4.0 }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/sensors/1"))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Câmara de carnes"))
                .andExpect(jsonPath("$.maxTemperature").value(4.0));
    }

    @Test
    void create_shouldReturn400WhenRequestIsInvalid() throws Exception {
        mockMvc.perform(post("/sensors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "name": "" }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation error"))
                .andExpect(jsonPath("$.errors.name").exists())
                .andExpect(jsonPath("$.errors.maxTemperature").exists());

        verify(sensorService, never()).create(any());
    }

    @Test
    void findAll_shouldReturn200WithAllSensors() throws Exception {
        when(sensorService.findAll()).thenReturn(List.of(
                new SensorResponse(1L, "Câmara de carnes", 4.0, CREATED_AT),
                new SensorResponse(2L, "Câmara de laticínios", 7.0, CREATED_AT)
        ));

        mockMvc.perform(get("/sensors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Câmara de carnes"))
                .andExpect(jsonPath("$[1].name").value("Câmara de laticínios"));
    }

    @Test
    void findById_shouldReturn200WhenSensorExists() throws Exception {
        when(sensorService.findById(1L))
                .thenReturn(new SensorResponse(1L, "Câmara de carnes", 4.0, CREATED_AT));

        mockMvc.perform(get("/sensors/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.createdAt").value("2026-10-13T12:00:00Z"));
    }

    @Test
    void findById_shouldReturn404WhenSensorDoesNotExist() throws Exception {
        when(sensorService.findById(99L)).thenThrow(new SensorNotFoundException(99L));

        mockMvc.perform(get("/sensors/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Sensor not found"))
                .andExpect(jsonPath("$.status").value(404));
    }
}
