package com.agrosentinel.agrosentinel.controllers;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.agrosentinel.agrosentinel.dtos.PredictiveAssetResponse;
import com.agrosentinel.agrosentinel.dtos.PredictiveRecordRequest;
import com.agrosentinel.agrosentinel.dtos.PredictiveRecordResponse;
import com.agrosentinel.agrosentinel.services.PredictiveService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/predictive")
@RequiredArgsConstructor
public class PredictiveController {
    private final PredictiveService predictiveService;

    @PostMapping("/records")
    @ResponseStatus(HttpStatus.CREATED)
    public PredictiveRecordResponse create(
            @Valid @RequestBody PredictiveRecordRequest request,
            @RequestHeader("X-Predictive-Token") String token) {
        return predictiveService.create(request, token);
    }

    @GetMapping("/assets")
    public List<PredictiveAssetResponse> motorAssets() {
        return predictiveService.motorAssets();
    }

    @GetMapping("/assets/{assetId}/history")
    public List<PredictiveRecordResponse> history(@PathVariable Long assetId) {
        return predictiveService.history(assetId);
    }
}