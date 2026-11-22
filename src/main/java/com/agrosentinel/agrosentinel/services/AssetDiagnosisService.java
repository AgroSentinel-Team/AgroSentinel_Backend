package com.agrosentinel.agrosentinel.services;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.agrosentinel.agrosentinel.dtos.AssetDiagnosisRequest;
import com.agrosentinel.agrosentinel.dtos.AssetDiagnosisResponse;
import com.agrosentinel.agrosentinel.dtos.DiagnosisNeedRequest;
import com.agrosentinel.agrosentinel.dtos.DiagnosisNeedResponse;
import com.agrosentinel.agrosentinel.dtos.DiagnosisOrderRequest;
import com.agrosentinel.agrosentinel.dtos.FailureReportRequest;
import com.agrosentinel.agrosentinel.dtos.PreventiveMaintenanceCreateRequest;
import com.agrosentinel.agrosentinel.models.Asset;
import com.agrosentinel.agrosentinel.models.AssetDiagnosis;
import com.agrosentinel.agrosentinel.models.DiagnosisNeed;
import com.agrosentinel.agrosentinel.models.DiagnosisRecommendation;
import com.agrosentinel.agrosentinel.models.FailurePriority;
import com.agrosentinel.agrosentinel.models.FailureStatus;
import com.agrosentinel.agrosentinel.models.InterventionType;
import com.agrosentinel.agrosentinel.models.MaintenanceAttentionIndex;
import com.agrosentinel.agrosentinel.models.NeedPriority;
import com.agrosentinel.agrosentinel.models.NeedStatus;
import com.agrosentinel.agrosentinel.models.OperationalStatus;
import com.agrosentinel.agrosentinel.models.PreventiveMaintenanceStatus;
import com.agrosentinel.agrosentinel.models.Usuario;
import com.agrosentinel.agrosentinel.repositories.AssetDiagnosisRepository;
import com.agrosentinel.agrosentinel.repositories.AssetRepository;
import com.agrosentinel.agrosentinel.repositories.DiagnosisNeedRepository;
import com.agrosentinel.agrosentinel.repositories.FailureReportRepository;
import com.agrosentinel.agrosentinel.repositories.PreventiveMaintenanceRepository;
import com.agrosentinel.agrosentinel.repositories.UsuarioRepository;

import lombok.RequiredArgsConstructor;

/**
 * Fichas técnicas de diagnóstico: estado del equipo, concepto técnico, clasificación,
 * recomendación (conservar, recuperar, evaluar baja...) y necesidades identificadas, que pueden
 * convertirse en órdenes correctivas o preventivas.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AssetDiagnosisService {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Set<OperationalStatus> DISPOSAL_CONDITIONS =
            Set.of(OperationalStatus.OUT_OF_SERVICE, OperationalStatus.OBSOLETE);

    private final AssetDiagnosisRepository diagnosisRepository;
    private final DiagnosisNeedRepository needRepository;
    private final AssetRepository assetRepository;
    private final UsuarioRepository usuarioRepository;
    private final FailureReportRepository failureReportRepository;
    private final PreventiveMaintenanceRepository preventiveMaintenanceRepository;
    private final FailureReportService failureReportService;
    private final PreventiveMaintenanceService preventiveMaintenanceService;

    // ---------- Consultas ----------

    public List<AssetDiagnosisResponse> findByAsset(Long assetId) {
        findAsset(assetId);
        return diagnosisRepository.findAllByAssetIdOrderByDiagnosedOnDescIdDesc(assetId).stream()
                .map(this::toResponse)
                .toList();
    }

    /** El diagnóstico vigente (más reciente) de cada activo, para la matriz consolidada. */
    public List<AssetDiagnosisResponse> findLatestPerAsset() {
        Map<Long, AssetDiagnosis> latest = new LinkedHashMap<>();
        for (AssetDiagnosis diagnosis : diagnosisRepository.findAllNewestFirstPerAsset()) {
            latest.putIfAbsent(diagnosis.getAsset().getId(), diagnosis);
        }
        return latest.values().stream().map(this::toResponse).toList();
    }

    public AssetDiagnosisResponse findById(Long id) {
        return toResponse(findDiagnosis(id));
    }

    // ---------- Registro ----------

    @Transactional
    public AssetDiagnosisResponse create(AssetDiagnosisRequest request, Authentication authentication) {
        Asset asset = findAsset(request.assetId());
        AssetDiagnosis diagnosis = new AssetDiagnosis();
        diagnosis.setAsset(asset);
        Usuario inspector = usuarioRepository.findByEmail(authentication.getName());
        diagnosis.setInspector(inspector);
        diagnosis.setInspectorName(inspector != null ? inspector.getNombre() : authentication.getName());
        applyRequest(diagnosis, request);
        AssetDiagnosis saved = diagnosisRepository.save(diagnosis);
        syncAssetStatus(saved, request.updateAssetStatus());
        return toResponse(saved);
    }

    @Transactional
    public AssetDiagnosisResponse update(Long id, AssetDiagnosisRequest request) {
        AssetDiagnosis diagnosis = findDiagnosis(id);
        if (!diagnosis.getAsset().getId().equals(request.assetId())) {
            throw badRequest("El diagnóstico pertenece a otro activo");
        }
        applyRequest(diagnosis, request);
        AssetDiagnosis saved = diagnosisRepository.save(diagnosis);
        syncAssetStatus(saved, request.updateAssetStatus());
        return toResponse(saved);
    }

    @Transactional
    public void delete(Long id) {
        diagnosisRepository.delete(findDiagnosis(id));
    }

    @Transactional
    public DiagnosisNeedResponse updateNeedStatus(Long needId, NeedStatus status) {
        DiagnosisNeed need = findNeed(needId);
        need.setStatus(status);
        return toNeedResponse(needRepository.save(need));
    }

    /** Crea la orden correctiva o preventiva de una necesidad y la deja vinculada. */
    @Transactional
    public DiagnosisNeedResponse createOrder(Long needId, DiagnosisOrderRequest request, Authentication authentication) {
        DiagnosisNeed need = findNeed(needId);
        if (need.getFailureReport() != null || need.getPreventiveMaintenance() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Esta necesidad ya tiene una orden de trabajo");
        }
        if (need.getStatus() == NeedStatus.DONE || need.getStatus() == NeedStatus.DISCARDED) {
            throw badRequest("La necesidad ya está cerrada");
        }
        AssetDiagnosis diagnosis = need.getDiagnosis();
        Asset asset = diagnosis.getAsset();
        String context = orderContext(need, diagnosis);

        if (request.type() == DiagnosisOrderRequest.OrderType.CORRECTIVE) {
            if (request.groupId() == null) {
                throw badRequest("Selecciona el grupo que atenderá la orden correctiva");
            }
            var report = failureReportService.create(
                    new FailureReportRequest(asset.getId(), truncate(need.getDescription(), 120),
                            toFailurePriority(need.getPriority()), context, request.groupId(), request.technicianId()),
                    null, authentication);
            need.setFailureReport(failureReportRepository.getReferenceById(report.id()));
        } else {
            if (request.technicianId() == null || request.scheduledAt() == null) {
                throw badRequest("Selecciona el técnico y la fecha del mantenimiento preventivo");
            }
            var maintenance = preventiveMaintenanceService.schedule(
                    new PreventiveMaintenanceCreateRequest(asset.getId(), request.technicianId(),
                            request.scheduledAt(), toAttentionIndex(need.getPriority()), context),
                    authentication);
            need.setPreventiveMaintenance(preventiveMaintenanceRepository.getReferenceById(maintenance.id()));
        }
        need.setStatus(NeedStatus.IN_PROGRESS);
        return toNeedResponse(needRepository.save(need));
    }

    // ---------- Reglas ----------

    private void applyRequest(AssetDiagnosis diagnosis, AssetDiagnosisRequest request) {
        LocalDate diagnosedOn = request.diagnosedOn() != null ? request.diagnosedOn() : LocalDate.now();
        if (diagnosedOn.isAfter(LocalDate.now())) {
            throw badRequest("La fecha del diagnóstico no puede ser futura");
        }
        validateClassification(request.operationalCondition(), request.recommendation(), request.disposalSupport());

        diagnosis.setDiagnosedOn(diagnosedOn);
        diagnosis.setCurrentStateDescription(trimToNull(request.currentStateDescription()));
        diagnosis.setTechnicalDiagnosis(trimToNull(request.technicalDiagnosis()));
        diagnosis.setTechnicalConcept(trimToNull(request.technicalConcept()));
        diagnosis.setConclusion(trimToNull(request.conclusion()));
        diagnosis.setOperationalCondition(request.operationalCondition());
        diagnosis.setRecommendation(request.recommendation());
        diagnosis.setRecommendationDetail(trimToNull(request.recommendationDetail()));
        diagnosis.setDisposalSupport(trimToNull(request.disposalSupport()));
        applyNeeds(diagnosis, request.needs() == null ? List.of() : request.needs());
    }

    /**
     * La recomendación debe ser coherente con la condición: la baja solo se considera para equipos
     * fuera de servicio u obsoletos, y recomendarla exige dejar el soporte técnico-económico.
     */
    static void validateClassification(OperationalStatus condition, DiagnosisRecommendation recommendation,
            String disposalSupport) {
        boolean disposal = recommendation == DiagnosisRecommendation.EVALUATE_DISPOSAL
                || recommendation == DiagnosisRecommendation.RECOMMENDED_DISPOSAL;
        if (disposal && !DISPOSAL_CONDITIONS.contains(condition)) {
            throw badRequest("Solo se puede recomendar evaluar la baja de un equipo fuera de servicio u obsoleto");
        }
        if (recommendation == DiagnosisRecommendation.RECOVER
                && (condition == OperationalStatus.OPERATIONAL
                        || condition == OperationalStatus.OPERATIONAL_WITH_RESTRICTIONS)) {
            throw badRequest("\"Recuperable\" aplica a equipos fuera de servicio o en reparación; "
                    + "para equipos operativos usa \"Conservar\"");
        }
        if (recommendation == DiagnosisRecommendation.RECOMMENDED_DISPOSAL
                && (disposalSupport == null || disposalSupport.isBlank())) {
            throw badRequest("Para recomendar la baja registra el soporte de la evaluación técnica y económica");
        }
    }

    /** Actualiza en su sitio las necesidades existentes (conservan su orden vinculada). */
    private void applyNeeds(AssetDiagnosis diagnosis, List<DiagnosisNeedRequest> requests) {
        Map<Long, DiagnosisNeed> current = new HashMap<>();
        diagnosis.getNeeds().forEach(need -> current.put(need.getId(), need));
        Set<DiagnosisNeed> kept = new HashSet<>();

        for (int index = 0; index < requests.size(); index++) {
            DiagnosisNeedRequest request = requests.get(index);
            DiagnosisNeed need = request.id() == null ? null : current.get(request.id());
            if (request.id() != null && need == null) {
                throw badRequest("La necesidad " + request.id() + " no pertenece a este diagnóstico");
            }
            if (need == null) {
                need = new DiagnosisNeed();
                need.setDiagnosis(diagnosis);
                diagnosis.getNeeds().add(need);
            }
            need.setDescription(request.description().trim());
            need.setInterventionTypes(request.interventionTypes().stream()
                    .distinct()
                    .map(InterventionType::name)
                    .collect(Collectors.joining(",")));
            need.setPriority(request.priority());
            need.setSafetyRelated(Boolean.TRUE.equals(request.safetyRelated()));
            if (request.status() != null) {
                need.setStatus(request.status());
            }
            need.setSortOrder(index);
            kept.add(need);
        }
        diagnosis.getNeeds().removeIf(need -> !kept.contains(need));
        diagnosis.getNeeds().sort((left, right) -> Integer.compare(left.getSortOrder(), right.getSortOrder()));
    }

    /** El diagnóstico más reciente del activo define su estado operativo. */
    private void syncAssetStatus(AssetDiagnosis diagnosis, Boolean updateAssetStatus) {
        if (Boolean.FALSE.equals(updateAssetStatus)) {
            return;
        }
        List<AssetDiagnosis> history =
                diagnosisRepository.findAllByAssetIdOrderByDiagnosedOnDescIdDesc(diagnosis.getAsset().getId());
        if (!history.isEmpty() && !history.getFirst().getId().equals(diagnosis.getId())) {
            return;
        }
        Asset asset = diagnosis.getAsset();
        asset.setOperationalStatus(diagnosis.getOperationalCondition());
        assetRepository.save(asset);
    }

    /** Etiqueta de clasificación como aparece en las fichas y en la matriz consolidada. */
    static String classificationLabel(OperationalStatus condition, DiagnosisRecommendation recommendation) {
        String base = switch (condition) {
            case OPERATIONAL -> "OPERATIVO – BUEN ESTADO";
            case OPERATIONAL_WITH_RESTRICTIONS -> "OPERATIVO – REQUIERE MANTENIMIENTO";
            case IN_REPAIR -> "EN REPARACIÓN";
            case OUT_OF_SERVICE -> "FUERA DE SERVICIO";
            case OBSOLETE -> "FUERA DE SERVICIO – OBSOLETO";
        };
        String suffix = switch (recommendation) {
            case KEEP -> null;
            case RECOVER -> "RECUPERABLE";
            case PENDING_EVALUATION -> "PENDIENTE DE EVALUACIÓN";
            case EVALUATE_DISPOSAL -> "RECOMENDADO PARA EVALUACIÓN DE BAJA";
            case RECOMMENDED_DISPOSAL -> "RECOMENDADO PARA BAJA";
        };
        return suffix == null ? base : base + " – " + suffix;
    }

    private static String orderContext(DiagnosisNeed need, AssetDiagnosis diagnosis) {
        StringBuilder text = new StringBuilder("Necesidad identificada en el diagnóstico del ")
                .append(diagnosis.getDiagnosedOn().format(DATE))
                .append(" (prioridad ").append(need.getPriority().name().toLowerCase())
                .append(need.isSafetyRelated() ? ", relacionada con la seguridad del operador" : "")
                .append("): ").append(need.getDescription()).append('.');
        if (diagnosis.getTechnicalDiagnosis() != null) {
            text.append("\n\nDiagnóstico técnico: ").append(diagnosis.getTechnicalDiagnosis());
        }
        return truncate(text.toString(), 5000);
    }

    private static FailurePriority toFailurePriority(NeedPriority priority) {
        return switch (priority) {
            case ALTA -> FailurePriority.ALTA;
            case MEDIA -> FailurePriority.MEDIA;
            case BAJA -> FailurePriority.BAJA;
        };
    }

    private static MaintenanceAttentionIndex toAttentionIndex(NeedPriority priority) {
        return switch (priority) {
            case ALTA -> MaintenanceAttentionIndex.HIGH;
            case MEDIA -> MaintenanceAttentionIndex.MEDIUM;
            case BAJA -> MaintenanceAttentionIndex.LOW;
        };
    }

    // ---------- Respuestas ----------

    private AssetDiagnosisResponse toResponse(AssetDiagnosis diagnosis) {
        return new AssetDiagnosisResponse(
                diagnosis.getId(), diagnosis.getAsset().getId(), diagnosis.getAsset().getName(),
                diagnosis.getDiagnosedOn(), diagnosis.getInspectorName(),
                diagnosis.getCurrentStateDescription(), diagnosis.getTechnicalDiagnosis(),
                diagnosis.getTechnicalConcept(), diagnosis.getConclusion(),
                diagnosis.getOperationalCondition(), diagnosis.getRecommendation(),
                classificationLabel(diagnosis.getOperationalCondition(), diagnosis.getRecommendation()),
                diagnosis.getRecommendationDetail(),
                diagnosis.getDisposalSupport(),
                diagnosis.getNeeds().stream().map(this::toNeedResponse).toList(),
                diagnosis.getCreatedAt(), diagnosis.getUpdatedAt());
    }

    private DiagnosisNeedResponse toNeedResponse(DiagnosisNeed need) {
        Long failureId = null;
        Long preventiveId = null;
        String orderLabel = null;
        String orderStatus = null;
        NeedStatus status = need.getStatus();
        if (need.getFailureReport() != null) {
            failureId = need.getFailureReport().getId();
            orderLabel = "F-" + failureId;
            FailureStatus failureStatus = need.getFailureReport().getEstado();
            orderStatus = failureStatus.name();
            if (failureStatus == FailureStatus.RESUELTO && status == NeedStatus.IN_PROGRESS) {
                status = NeedStatus.DONE;
            }
        } else if (need.getPreventiveMaintenance() != null) {
            preventiveId = need.getPreventiveMaintenance().getId();
            orderLabel = "MP-" + preventiveId;
            PreventiveMaintenanceStatus maintenanceStatus = need.getPreventiveMaintenance().getStatus();
            orderStatus = maintenanceStatus.name();
            if (maintenanceStatus == PreventiveMaintenanceStatus.COMPLETED && status == NeedStatus.IN_PROGRESS) {
                status = NeedStatus.DONE;
            }
        }
        List<InterventionType> types = Arrays.stream(need.getInterventionTypes().split(","))
                .filter(value -> !value.isBlank())
                .map(InterventionType::valueOf)
                .toList();
        return new DiagnosisNeedResponse(need.getId(), need.getDescription(), types, need.getPriority(),
                need.isSafetyRelated(), status, failureId, preventiveId, orderLabel, orderStatus);
    }

    // ---------- Utilidades ----------

    private Asset findAsset(Long id) {
        return assetRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Activo no encontrado"));
    }

    private AssetDiagnosis findDiagnosis(Long id) {
        return diagnosisRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Diagnóstico no encontrado"));
    }

    private DiagnosisNeed findNeed(Long id) {
        return needRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Necesidad no encontrada"));
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String truncate(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max - 1) + "…";
    }

    private static ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
