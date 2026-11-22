package com.agrosentinel.agrosentinel.controllers;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.agrosentinel.agrosentinel.dtos.AssetDiagnosisRequest;
import com.agrosentinel.agrosentinel.dtos.AssetDiagnosisResponse;
import com.agrosentinel.agrosentinel.dtos.DiagnosisNeedResponse;
import com.agrosentinel.agrosentinel.dtos.DiagnosisNeedStatusRequest;
import com.agrosentinel.agrosentinel.dtos.DiagnosisOrderRequest;
import com.agrosentinel.agrosentinel.services.AssetDiagnosisService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/asset-diagnoses")
@CrossOrigin(origins = "http://localhost:5173")
@RequiredArgsConstructor
public class AssetDiagnosisController {
    private final AssetDiagnosisService diagnosisService;

    /** Historial de diagnósticos de un activo, del más reciente al más antiguo. */
    @GetMapping
    public List<AssetDiagnosisResponse> findByAsset(@RequestParam Long assetId) {
        return diagnosisService.findByAsset(assetId);
    }

    /** Diagnóstico vigente de cada activo (matriz consolidada). */
    @GetMapping("/latest")
    public List<AssetDiagnosisResponse> findLatestPerAsset() {
        return diagnosisService.findLatestPerAsset();
    }

    @GetMapping("/{id}")
    public AssetDiagnosisResponse findById(@PathVariable Long id) {
        return diagnosisService.findById(id);
    }

    @PostMapping
    public ResponseEntity<AssetDiagnosisResponse> create(
            @Valid @RequestBody AssetDiagnosisRequest request, Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(diagnosisService.create(request, authentication));
    }

    @PutMapping("/{id}")
    public AssetDiagnosisResponse update(@PathVariable Long id, @Valid @RequestBody AssetDiagnosisRequest request) {
        return diagnosisService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        diagnosisService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/needs/{needId}/status")
    public DiagnosisNeedResponse updateNeedStatus(
            @PathVariable Long needId, @Valid @RequestBody DiagnosisNeedStatusRequest request) {
        return diagnosisService.updateNeedStatus(needId, request.status());
    }

    @PostMapping("/needs/{needId}/order")
    public DiagnosisNeedResponse createOrder(@PathVariable Long needId,
            @Valid @RequestBody DiagnosisOrderRequest request, Authentication authentication) {
        return diagnosisService.createOrder(needId, request, authentication);
    }
}
