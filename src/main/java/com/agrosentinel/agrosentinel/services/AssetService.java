package com.agrosentinel.agrosentinel.services;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.agrosentinel.agrosentinel.dtos.AssetRequest;
import com.agrosentinel.agrosentinel.dtos.AssetResponse;
import com.agrosentinel.agrosentinel.dtos.AssetResumeResponse;
import com.agrosentinel.agrosentinel.dtos.MaintenanceHistoryResponse;
import com.agrosentinel.agrosentinel.models.Asset;
import com.agrosentinel.agrosentinel.models.Category;
import com.agrosentinel.agrosentinel.models.MaintenanceHistory;
import com.agrosentinel.agrosentinel.models.MaintenanceHistoryStatus;
import com.agrosentinel.agrosentinel.models.MaintenanceType;
import com.agrosentinel.agrosentinel.models.OperationalStatus;
import com.agrosentinel.agrosentinel.models.PreventiveMaintenanceStatus;
import com.agrosentinel.agrosentinel.models.Usuario;
import com.agrosentinel.agrosentinel.repositories.AssetRepository;
import com.agrosentinel.agrosentinel.repositories.CategoryRepository;
import com.agrosentinel.agrosentinel.repositories.MaintenanceHistoryRepository;
import com.agrosentinel.agrosentinel.repositories.PreventiveMaintenanceRepository;
import com.agrosentinel.agrosentinel.repositories.UsuarioRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AssetService {
    private final AssetRepository assetRepository;
    private final CategoryRepository categoryRepository;
    private final UsuarioRepository usuarioRepository;
    private final MaintenanceHistoryRepository maintenanceHistoryRepository;
    private final PreventiveMaintenanceRepository preventiveMaintenanceRepository;

    public List<AssetResponse> findAll() {
        return assetRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional
    public AssetResponse create(AssetRequest request) {
        Asset asset = new Asset();
        applyRequest(asset, request);
        return toResponse(assetRepository.save(asset));
    }

    @Transactional
    public AssetResponse update(Long id, AssetRequest request) {
        Asset asset = findAsset(id);
        applyRequest(asset, request);
        return toResponse(assetRepository.save(asset));
    }

    @Transactional
    public AssetResponse deactivate(Long id) {
        Asset asset = findAsset(id);
        asset.setActive(false);
        return toResponse(assetRepository.save(asset));
    }

    @Transactional
    public void softDelete(Long id) {
        Asset asset = findAsset(id);
        asset.setDeletedAt(LocalDateTime.now());
        assetRepository.save(asset);
    }

    @Transactional
    public AssetResumeResponse getResume(Long id) {
        Asset asset = findAsset(id);
        ensureScheduledPreventiveHistory(asset);
        List<MaintenanceHistoryResponse> history = maintenanceHistoryRepository
                .findAllByAssetIdOrderByMaintenanceDateDesc(id)
                .stream()
                .map(this::toHistoryResponse)
                .toList();
        return new AssetResumeResponse(toResponse(asset), history);
    }

    private void ensureScheduledPreventiveHistory(Asset asset) {
        preventiveMaintenanceRepository
                .findAllByAssetIdAndStatusOrderByScheduledAtAsc(
                        asset.getId(), PreventiveMaintenanceStatus.SCHEDULED)
                .forEach(maintenance -> {
                    if (maintenanceHistoryRepository.findByPreventiveMaintenanceId(maintenance.getId()).isPresent()) {
                        return;
                    }

                    MaintenanceHistory history = new MaintenanceHistory();
                    history.setAsset(asset);
                    history.setPreventiveMaintenance(maintenance);
                    history.setType(MaintenanceType.PREVENTIVE);
                    history.setStatus(MaintenanceHistoryStatus.SCHEDULED);
                    history.setMaintenanceDate(maintenance.getScheduledAt().toLocalDate());
                    history.setScheduledAt(maintenance.getScheduledAt());
                    history.setTechnicianName(maintenance.getTechnician().getNombre());
                    history.setTasksDescription(
                            "Mantenimiento preventivo programado para " + asset.getName() + ".");
                    maintenanceHistoryRepository.save(history);
                });
    }

    private void applyRequest(Asset asset, AssetRequest request) {
        asset.setName(request.name());
        asset.setBrand(request.brand());
        asset.setModel(request.model());
        asset.setSerialNumber(request.serialNumber());
        asset.setExtraSpecifications(request.extraSpecifications());
        asset.setCategory(findCategory(request.categoryId()));
        asset.setResponsible(findResponsible(request.responsibleId()));
        asset.setAcquisitionDate(request.acquisitionDate());
        asset.setAcquisitionCost(request.acquisitionCost());
        asset.setProvider(request.provider());
        asset.setWarrantyExpirationDate(request.warrantyExpirationDate());
        asset.setLocation(request.location());
        asset.setOperationalStatus(request.operationalStatus() == null
                ? OperationalStatus.OPERATIONAL
                : request.operationalStatus());
        if (request.isActive() != null) {
            asset.setActive(request.isActive());
        }
        if (request.hasMotor() != null) {
            asset.setHasMotor(request.hasMotor());
        }
    }

    private Asset findAsset(Long id) {
        return assetRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Activo no encontrado"));
    }

    private Category findCategory(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Categoría no encontrada"));
    }

    private Usuario findResponsible(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Responsable no encontrado"));
    }

    private AssetResponse toResponse(Asset asset) {
        return new AssetResponse(
                asset.getId(), asset.getName(), asset.getBrand(), asset.getModel(), asset.getSerialNumber(),
                asset.getExtraSpecifications(), asset.getCategory().getId(), asset.getCategory().getName(),
                asset.getResponsible().getId(), asset.getResponsible().getNombre(), asset.getAcquisitionDate(),
                asset.getAcquisitionCost(), asset.getProvider(), asset.getWarrantyExpirationDate(),
                asset.getLocation(), asset.getOperationalStatus(), asset.isActive(), asset.isHasMotor(),
                asset.isHasMotor() ? "MOTOR-" + asset.getId() : null);
    }

    private MaintenanceHistoryResponse toHistoryResponse(MaintenanceHistory history) {
        return new MaintenanceHistoryResponse(
                history.getId(), history.getMaintenanceDate(), history.getType(), history.getTasksDescription(),
                history.getCost(), history.getProvider(), history.getNextRecommendedDate(), history.getStatus(),
                history.getScheduledAt(), history.getExecutedAt(), history.getTechnicianName(),
                history.getReplacedParts(), history.getSignedAt(),
                history.getPreventiveMaintenance() == null
                        ? List.of()
                        : history.getPreventiveMaintenance().getEvidences().stream()
                                .map(evidence -> evidence.getUrl())
                                .toList());
    }
}