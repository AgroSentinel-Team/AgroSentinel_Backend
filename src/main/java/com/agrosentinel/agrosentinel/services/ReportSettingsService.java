package com.agrosentinel.agrosentinel.services;

import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.agrosentinel.agrosentinel.models.ReportSettings;
import com.agrosentinel.agrosentinel.repositories.ReportSettingsRepository;

import lombok.RequiredArgsConstructor;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Configuración institucional de los informes (nombre de la institución, logos en base64,
 * textos de introducción, firma...). El frontend define su forma; aquí solo se valida que sea
 * un objeto JSON de tamaño razonable y se comparte entre todos los administradores.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportSettingsService {
    /** Suficiente para dos logos comprimidos y los textos del informe. */
    static final int MAX_SETTINGS_LENGTH = 3_000_000;

    private final ReportSettingsRepository repository;
    private final ObjectMapper objectMapper;

    public String getSettingsJson() {
        return repository.findById(ReportSettings.SINGLE_ID)
                .map(ReportSettings::getSettingsJson)
                .orElse("{}");
    }

    @Transactional
    public String save(JsonNode settings, String updatedBy) {
        if (settings == null || !settings.isObject()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La configuración debe ser un objeto JSON");
        }
        String json = objectMapper.writeValueAsString(settings);
        if (json.length() > MAX_SETTINGS_LENGTH) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,
                    "La configuración es demasiado grande. Usa logos más livianos (menos de 1 MB cada uno).");
        }
        ReportSettings entity = repository.findById(ReportSettings.SINGLE_ID).orElseGet(ReportSettings::new);
        entity.setSettingsJson(json);
        entity.setUpdatedBy(updatedBy);
        entity.setUpdatedAt(LocalDateTime.now());
        return repository.save(entity).getSettingsJson();
    }
}
