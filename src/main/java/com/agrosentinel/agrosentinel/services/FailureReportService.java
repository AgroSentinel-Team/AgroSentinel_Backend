package com.agrosentinel.agrosentinel.services;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.agrosentinel.agrosentinel.dtos.FailureAssetOption;
import com.agrosentinel.agrosentinel.dtos.FailureReportRequest;
import com.agrosentinel.agrosentinel.dtos.FailureReportResponse;
import com.agrosentinel.agrosentinel.models.Asset;
import com.agrosentinel.agrosentinel.models.FailurePriority;
import com.agrosentinel.agrosentinel.models.FailureReport;
import com.agrosentinel.agrosentinel.models.FailureStatus;
import com.agrosentinel.agrosentinel.models.OperationalStatus;
import com.agrosentinel.agrosentinel.models.Usuario;
import com.agrosentinel.agrosentinel.repositories.AssetRepository;
import com.agrosentinel.agrosentinel.repositories.FailureReportRepository;
import com.agrosentinel.agrosentinel.repositories.UsuarioRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FailureReportService {
    private final FailureReportRepository failureReportRepository;
    private final AssetRepository assetRepository;
    private final UsuarioRepository usuarioRepository;
    private final FailureEvidenceStorageService evidenceStorageService;

    public List<FailureReportResponse> findAll(String currentUserEmail, boolean canViewAll) {
        List<FailureReport> reports;
        if (canViewAll) {
            reports = failureReportRepository.findAllByOrderByFechaReporteDesc();
        } else {
            Usuario user = usuarioRepository.findByEmail(currentUserEmail);
            if (user == null) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario autenticado no disponible");
            }
            reports = failureReportRepository.findAllByUserIdOrderByFechaReporteDesc(user.getId());
        }
        return reports.stream().map(this::toResponse).toList();
    }

    public List<FailureAssetOption> findAssets(String currentUserEmail) {
        return assetRepository.findAll().stream()
                .filter(Asset::isActive)
                .map(asset -> new FailureAssetOption(
                        asset.getId(), asset.getName(), asset.getBrand(), asset.getModel(), asset.getSerialNumber(),
                        asset.getLocation(), asset.getResponsible().getId(), asset.getResponsible().getNombre(),
                        asset.getOperationalStatus(), currentUserEmail.equalsIgnoreCase(asset.getResponsible().getEmail())))
                .toList();
    }

    @Transactional
    public FailureReportResponse create(FailureReportRequest request, MultipartFile evidence, String reporterEmail) {
        Asset asset = assetRepository.findById(request.assetId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Activo no encontrado"));
        Usuario reporter = usuarioRepository.findByEmail(reporterEmail);
        if (reporter == null || Boolean.FALSE.equals(reporter.getActivo())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario autenticado no disponible");
        }

        FailureReport report = new FailureReport();
        report.setAsset(asset);
        report.setUser(reporter);
        report.setTipoFalla(request.tipoFalla().trim());
        report.setPrioridad(request.prioridad());
        report.setDescripcion(request.descripcion().trim());
        report.setFechaReporte(LocalDateTime.now());
        report.setEstado(FailureStatus.PENDIENTE);
        report.setEvidenciaUrl(evidenceStorageService.store(evidence));

        if (request.prioridad() == FailurePriority.ALTA || request.prioridad() == FailurePriority.CRITICA) {
            asset.setOperationalStatus(OperationalStatus.IN_REPAIR);
        }

        return toResponse(failureReportRepository.save(report));
    }

    private FailureReportResponse toResponse(FailureReport report) {
        return new FailureReportResponse(
                report.getId(), report.getAsset().getId(), report.getAsset().getName(),
                report.getUser().getId(), report.getUser().getNombre(), report.getTipoFalla(),
                report.getPrioridad(), report.getDescripcion(), report.getFechaReporte(), report.getEstado(),
                report.getEvidenciaUrl());
    }
}
