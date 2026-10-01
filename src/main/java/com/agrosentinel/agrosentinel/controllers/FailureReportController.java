package com.agrosentinel.agrosentinel.controllers;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.agrosentinel.agrosentinel.dtos.FailureAssetOption;
import com.agrosentinel.agrosentinel.dtos.FailureReportRequest;
import com.agrosentinel.agrosentinel.dtos.FailureReportResponse;
import com.agrosentinel.agrosentinel.services.FailureReportService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/failure-reports")
@RequiredArgsConstructor
public class FailureReportController {
    private final FailureReportService failureReportService;

    @GetMapping
    public List<FailureReportResponse> findAll(Authentication authentication) {
        boolean canViewAll = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority -> authority.equals("ROLE_ADMINISTRADOR") || authority.equals("ROLE_ADMIN"));
        return failureReportService.findAll(authentication.getName(), canViewAll);
    }

    @GetMapping("/assets")
    public List<FailureAssetOption> findAssets(Authentication authentication) {
        return failureReportService.findAssets(authentication.getName());
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public FailureReportResponse create(
            @RequestPart("data") @Valid FailureReportRequest request,
            @RequestPart(value = "evidencia", required = false) MultipartFile evidence,
            Authentication authentication) {
        return failureReportService.create(request, evidence, authentication.getName());
    }
}
