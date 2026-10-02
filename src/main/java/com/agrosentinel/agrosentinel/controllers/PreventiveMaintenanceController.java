package com.agrosentinel.agrosentinel.controllers;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.agrosentinel.agrosentinel.dtos.PreventiveMaintenanceCreateRequest;
import com.agrosentinel.agrosentinel.dtos.PreventiveMaintenanceExecutionRequest;
import com.agrosentinel.agrosentinel.dtos.PreventiveMaintenanceResponse;
import com.agrosentinel.agrosentinel.services.PreventiveMaintenanceService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/preventive-maintenances")
@RequiredArgsConstructor
public class PreventiveMaintenanceController {
    private final PreventiveMaintenanceService maintenanceService;

    @GetMapping
    public List<PreventiveMaintenanceResponse> findAll(Authentication authentication) {
        return maintenanceService.findAll(authentication);
    }

    @GetMapping("/{id}")
    public PreventiveMaintenanceResponse findById(@PathVariable Long id, Authentication authentication) {
        return maintenanceService.findById(id, authentication);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PreventiveMaintenanceResponse schedule(
            @Valid @RequestBody PreventiveMaintenanceCreateRequest request,
            Authentication authentication) {
        return maintenanceService.schedule(request, authentication);
    }

    @PutMapping(value = "/{id}/execution", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public PreventiveMaintenanceResponse execute(
            @PathVariable Long id,
            @RequestPart("data") @Valid PreventiveMaintenanceExecutionRequest request,
            @RequestPart(value = "evidencias", required = false) List<MultipartFile> evidences,
            Authentication authentication) {
        return maintenanceService.execute(id, request, evidences, authentication);
    }
}
