package com.agrosentinel.agrosentinel.controllers;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.agrosentinel.agrosentinel.services.ReportSettingsService;

import lombok.RequiredArgsConstructor;
import tools.jackson.databind.JsonNode;

@RestController
@RequestMapping("/api/report-settings")
@CrossOrigin(origins = "http://localhost:5173")
@RequiredArgsConstructor
public class ReportSettingsController {
    private final ReportSettingsService settingsService;

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> get() {
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(settingsService.getSettingsJson());
    }

    @PutMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> save(@RequestBody JsonNode settings, Authentication authentication) {
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON)
                .body(settingsService.save(settings, authentication.getName()));
    }
}
