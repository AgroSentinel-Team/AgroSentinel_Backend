package com.agrosentinel.agrosentinel.services;

import java.time.LocalDateTime;
import java.time.Year;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.agrosentinel.agrosentinel.dtos.AssetDuplicateResponse;
import com.agrosentinel.agrosentinel.dtos.AssetRequest;
import com.agrosentinel.agrosentinel.dtos.AssetFailureTrendResponse;
import com.agrosentinel.agrosentinel.dtos.AssetResponse;
import com.agrosentinel.agrosentinel.dtos.AssetResumeResponse;
import com.agrosentinel.agrosentinel.dtos.MaintenanceHistoryResponse;
import com.agrosentinel.agrosentinel.dtos.MaintenancePartResponse;
import com.agrosentinel.agrosentinel.models.Asset;
import com.agrosentinel.agrosentinel.models.Category;
import com.agrosentinel.agrosentinel.models.CategorySpecField;
import com.agrosentinel.agrosentinel.models.InventoryCodeStatus;
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
    private final AssetImageStorageService assetImageStorageService;
    private final AssetSpecificationService specificationService;

    public List<AssetResponse> findAll() {
        Map<Long, List<CategorySpecField>> templates = specificationService.templatesByCategory();
        return assetRepository.findAll().stream()
                .map(asset -> toResponse(asset, templates.getOrDefault(asset.getCategory().getId(), List.of())))
                .toList();
    }

    public List<AssetResponse> findAssignedToUser(String email) {
        Map<Long, List<CategorySpecField>> templates = specificationService.templatesByCategory();
        return assetRepository.findAll().stream()
                .filter(Asset::isActive)
                .filter(asset -> email.equalsIgnoreCase(asset.getResponsible().getEmail()))
                .map(asset -> toResponse(asset, templates.getOrDefault(asset.getCategory().getId(), List.of())))
                .toList();
    }

    /**
     * Activos que ya usan el serial o el código de inventario indicados. No bloquea el registro:
     * sirve para advertir al usuario antes de guardar.
     */
    public AssetDuplicateResponse findDuplicates(String serialNumber, String inventoryCode, Long excludeId) {
        return new AssetDuplicateResponse(
                duplicateMatches(isBlank(serialNumber) ? List.of()
                        : assetRepository.findAllBySerialNumberIgnoreCase(serialNumber.trim()), excludeId),
                duplicateMatches(isBlank(inventoryCode) ? List.of()
                        : assetRepository.findAllByInventoryCodeIgnoreCase(inventoryCode.trim()), excludeId));
    }

    private List<AssetDuplicateResponse.Match> duplicateMatches(List<Asset> assets, Long excludeId) {
        return assets.stream()
                .filter(asset -> excludeId == null || !excludeId.equals(asset.getId()))
                .map(asset -> new AssetDuplicateResponse.Match(asset.getId(), asset.getName(),
                        asset.getSerialNumber(), asset.getInventoryCode()))
                .toList();
    }

    public AssetResumeResponse getAssignedResume(Long id, String email) {
        Asset asset = findAsset(id);
        if (!asset.isActive() || !email.equalsIgnoreCase(asset.getResponsible().getEmail())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo puedes consultar la hoja de vida de maquinaria asignada a tu usuario");
        }
        ensureScheduledPreventiveHistory(asset);
        List<MaintenanceHistoryResponse> history = maintenanceHistoryRepository
                .findAllByAssetIdOrderByMaintenanceDateDesc(id)
                .stream()
                .map(this::toHistoryResponse)
                .toList();
        return new AssetResumeResponse(toResponse(asset), history);
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
    public AssetResponse updateImage(Long id, MultipartFile image) {
        Asset asset = findAsset(id);
        asset.setImageUrl(assetImageStorageService.store(image));
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

    public AssetFailureTrendResponse getFailureTrends(Long id) {
        Asset asset = findAsset(id);
        List<MaintenanceHistory> failures = maintenanceHistoryRepository
                .findAllByAssetIdOrderByMaintenanceDateDesc(id).stream()
                .filter(history -> history.getType() == MaintenanceType.CORRECTIVE)
                .filter(history -> history.getMaintenanceDate() != null)
                .sorted(java.util.Comparator.comparing(MaintenanceHistory::getMaintenanceDate))
                .toList();
        int baseInterval = asset.getCategory().getMaintenanceFrequencyDays() > 0
                ? asset.getCategory().getMaintenanceFrequencyDays()
                : 90;
        Double averageDays = null;
        int suggestedInterval = baseInterval;
        if (failures.size() >= 2) {
            long totalDays = 0;
            for (int index = 1; index < failures.size(); index++) {
                totalDays += ChronoUnit.DAYS.between(
                        failures.get(index - 1).getMaintenanceDate(),
                        failures.get(index).getMaintenanceDate());
            }
            averageDays = (double) totalDays / (failures.size() - 1);
            suggestedInterval = Math.min(
                    baseInterval, Math.max(7, (int) Math.round(averageDays * 0.8)));
        }

        Map<String, Long> repeated = failures.stream()
                .map(MaintenanceHistory::getActualFailure)
                .filter(value -> value != null && !value.isBlank())
                .collect(Collectors.groupingBy(
                        value -> value.trim().toLowerCase(java.util.Locale.ROOT),
                        Collectors.counting()));
        List<String> recurringFailures = failures.stream()
                .map(MaintenanceHistory::getActualFailure)
                .filter(value -> value != null && !value.isBlank())
                .filter(value -> repeated.get(value.trim().toLowerCase(java.util.Locale.ROOT)) > 1)
                .map(String::trim)
                .distinct()
                .toList();
        String summary = failures.size() < 2
                ? "Historial insuficiente para calcular tendencias; se conserva la frecuencia base de la categoría."
                : recurringFailures.isEmpty()
                        ? "El intervalo sugerido se ajustó con el promedio de tiempo entre fallas históricas."
                        : "Se repiten fallas similares. Se recomienda revisar las causas recurrentes y usar un intervalo preventivo más corto.";

        return new AssetFailureTrendResponse(
                asset.getId(),
                failures.size(),
                averageDays,
                baseInterval,
                suggestedInterval,
                asset.getCurrentHorometer() == null ? java.math.BigDecimal.ZERO : asset.getCurrentHorometer(),
                recurringFailures,
                summary);
    }

    public List<MaintenanceHistoryResponse> getTechnicianHistory(Long technicianId) {
        return maintenanceHistoryRepository.findAllExecutedByTechnicianId(technicianId).stream()
                .map(this::toHistoryResponse)
                .toList();
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
        asset.setName(request.name().trim());
        asset.setBrand(blankToNull(request.brand()));
        asset.setModel(blankToNull(request.model()));
        asset.setSerialNumber(blankToNull(request.serialNumber()));
        asset.setInventoryCode(blankToNull(request.inventoryCode()));
        asset.setInventoryCodeStatus(resolveInventoryCodeStatus(asset.getInventoryCode(), request.inventoryCodeStatus()));
        asset.setEquipmentType(blankToNull(request.equipmentType()));
        asset.setManufactureYear(validateYear(request.manufactureYear()));
        asset.setExtraSpecifications(request.extraSpecifications());
        asset.setCategory(findCategory(request.categoryId()));
        if (request.specifications() != null) {
            specificationService.applySpecifications(asset, request.specifications(),
                    specificationService.templateFor(asset.getCategory().getId()));
        }
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

    /** Con código registrado siempre es ASSIGNED; sin código, PENDING salvo que se indique NOT_PROVIDED. */
    static InventoryCodeStatus resolveInventoryCodeStatus(String inventoryCode, InventoryCodeStatus requested) {
        if (!isBlank(inventoryCode)) {
            return InventoryCodeStatus.ASSIGNED;
        }
        return requested == InventoryCodeStatus.NOT_PROVIDED
                ? InventoryCodeStatus.NOT_PROVIDED
                : InventoryCodeStatus.PENDING;
    }

    private static Integer validateYear(Integer year) {
        if (year == null) {
            return null;
        }
        int maxYear = Year.now().getValue() + 1;
        if (year < 1900 || year > maxYear) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El año de fabricación debe estar entre 1900 y " + maxYear);
        }
        return year;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String blankToNull(String value) {
        return isBlank(value) ? null : value.trim();
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
        return toResponse(asset, specificationService.templateFor(asset.getCategory().getId()));
    }

    private AssetResponse toResponse(Asset asset, List<CategorySpecField> template) {
        AssetSpecificationService.Completeness completeness = specificationService.completeness(asset, template);
        return new AssetResponse(
                asset.getId(), asset.getName(), asset.getBrand(), asset.getModel(), asset.getSerialNumber(),
                asset.getInventoryCode(), asset.getExtraSpecifications(), asset.getCategory().getId(), asset.getCategory().getName(),
                asset.getResponsible().getId(), asset.getResponsible().getNombre(), asset.getAcquisitionDate(),
                asset.getAcquisitionCost(), asset.getProvider(), asset.getWarrantyExpirationDate(),
                asset.getLocation(), asset.getOperationalStatus(), asset.isActive(), asset.isHasMotor(),
                asset.isHasMotor() ? "MOTOR-" + asset.getId() : null, asset.getImageUrl(),
                asset.getCurrentHorometer() == null ? java.math.BigDecimal.ZERO : asset.getCurrentHorometer(),
                asset.getEquipmentType(), asset.getManufactureYear(), asset.getInventoryCodeStatus(),
                specificationService.toResponses(asset, template), completeness.percent(),
                completeness.missingFields());
    }

    public MaintenanceHistoryResponse toHistoryResponse(MaintenanceHistory history) {
        return new MaintenanceHistoryResponse(
                history.getId(), history.getMaintenanceDate(), history.getType(), history.getTasksDescription(),
                history.getCost(), history.getProvider(), history.getNextRecommendedDate(), history.getStatus(),
                history.getScheduledAt(), history.getExecutedAt(), history.getTechnicianName(),
                history.getReplacedParts(), history.getSignedAt(),
                historyEvidence(history),
                history.getFailureReport() == null ? null : history.getFailureReport().getId(),
                history.getEquipmentIntervened(),
                history.getInitialReportedSymptom(),
                history.getInterventionReportedAt(),
                history.getInspectionStartedAt(),
                history.getActualFailure(),
                history.getRootCause(),
                history.getWorkMinutes(),
                history.getParts().stream()
                        .map(part -> new MaintenancePartResponse(
                                part.getPartNumber(), part.getName(), part.getBrand(), part.getQuantity()))
                        .toList(),
                history.getTestsPerformed(),
                history.getFinalEquipmentState(),
                history.getRecommendations(),
                history.getPendingTasks(),
                history.getReceivedBy(),
                history.getAsset().getId(),
                history.getAsset().getName(),
                history.getReviewObservations(),
                history.getReviewedBy(),
                history.getReviewedAt(),
                history.getHorometerAtMaintenance());
    }

    private List<String> historyEvidence(MaintenanceHistory history) {
        List<String> evidences = new java.util.ArrayList<>(history.getEvidences());
        if (history.getPreventiveMaintenance() != null) {
            history.getPreventiveMaintenance().getEvidences().stream()
                    .map(evidence -> evidence.getUrl())
                    .forEach(evidences::add);
        }
        if (history.getFailureReport() != null && history.getFailureReport().getEvidenciaUrl() != null) {
            evidences.add(history.getFailureReport().getEvidenciaUrl());
        }
        return List.copyOf(evidences);
    }
}