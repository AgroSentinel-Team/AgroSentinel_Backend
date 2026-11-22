package com.agrosentinel.agrosentinel.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import com.agrosentinel.agrosentinel.models.ReportSettings;
import com.agrosentinel.agrosentinel.repositories.ReportSettingsRepository;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

class ReportSettingsServiceTest {
    private final ReportSettingsRepository repository = mock(ReportSettingsRepository.class);
    private final ObjectMapper mapper = JsonMapper.builder().build();
    private final ReportSettingsService service = new ReportSettingsService(repository, mapper);

    @Test
    void returnsAnEmptyObjectWhenNothingIsSaved() {
        when(repository.findById(ReportSettings.SINGLE_ID)).thenReturn(Optional.empty());
        assertEquals("{}", service.getSettingsJson());
    }

    @Test
    void savesObjectsAndRejectsOtherJson() {
        when(repository.findById(ReportSettings.SINGLE_ID)).thenReturn(Optional.empty());
        when(repository.save(any(ReportSettings.class))).thenAnswer(invocation -> invocation.getArgument(0));

        String saved = service.save(mapper.readTree("{\"institutionName\":\"Granja\"}"), "admin@campo.test");

        assertEquals("{\"institutionName\":\"Granja\"}", saved);
        assertThrows(ResponseStatusException.class, () -> service.save(mapper.readTree("[1,2]"), "admin"));
        String huge = "{\"logo\":\"" + "a".repeat(ReportSettingsService.MAX_SETTINGS_LENGTH) + "\"}";
        assertThrows(ResponseStatusException.class, () -> service.save(mapper.readTree(huge), "admin"));
    }
}
