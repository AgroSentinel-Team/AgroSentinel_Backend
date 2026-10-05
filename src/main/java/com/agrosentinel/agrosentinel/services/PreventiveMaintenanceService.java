package com.agrosentinel.agrosentinel.services;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.agrosentinel.agrosentinel.dtos.PreventiveMaintenanceCreateRequest;
import com.agrosentinel.agrosentinel.dtos.PreventiveMaintenanceExecutionRequest;
import com.agrosentinel.agrosentinel.dtos.PreventiveMaintenanceResponse;
import com.agrosentinel.agrosentinel.dtos.PreventiveMaintenanceTaskRequest;
import com.agrosentinel.agrosentinel.dtos.PreventiveMaintenanceTaskResponse;
import com.agrosentinel.agrosentinel.models.Asset;
import com.agrosentinel.agrosentinel.models.MaintenanceAttentionIndex;
import com.agrosentinel.agrosentinel.models.MaintenanceHistory;
import com.agrosentinel.agrosentinel.models.MaintenanceHistoryStatus;
import com.agrosentinel.agrosentinel.models.MaintenanceOutcome;
import com.agrosentinel.agrosentinel.models.MaintenanceType;
import com.agrosentinel.agrosentinel.models.PreventiveMaintenance;
import com.agrosentinel.agrosentinel.models.PreventiveMaintenanceEvidence;
import com.agrosentinel.agrosentinel.models.PreventiveMaintenanceStatus;
import com.agrosentinel.agrosentinel.models.PreventiveMaintenanceTask;
import com.agrosentinel.agrosentinel.models.Usuario;
import com.agrosentinel.agrosentinel.repositories.AssetRepository;
import com.agrosentinel.agrosentinel.repositories.MaintenanceHistoryRepository;
import com.agrosentinel.agrosentinel.repositories.PreventiveMaintenanceRepository;
import com.agrosentinel.agrosentinel.repositories.UsuarioRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PreventiveMaintenanceService {
    private final PreventiveMaintenanceRepository maintenanceRepository;
    private final AssetRepository assetRepository;
    private final UsuarioRepository usuarioRepository;
    private final MaintenanceHistoryRepository maintenanceHistoryRepository;
    private final PreventiveMaintenanceEvidenceStorageService evidenceStorageService;
    private final UserRoleService userRoleService;

    public List<PreventiveMaintenanceResponse> findAll(Authentication authentication) {
        List<PreventiveMaintenance> maintenances = isAdministrator(authentication)
                ? maintenanceRepository.findAllByOrderByScheduledAtAsc()
                : maintenanceRepository.findAllByTechnician_EmailOrderByScheduledAtAsc(authentication.getName());
        return maintenances.stream().map(this::toResponse).toList();
    }

    public PreventiveMaintenanceResponse findById(Long id, Authentication authentication) {
        return toResponse(requireVisibleMaintenance(id, authentication));
    }

    @Transactional
    public PreventiveMaintenanceResponse schedule(
            PreventiveMaintenanceCreateRequest request, Authentication authentication) {
        if (!isAdministrator(authentication)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo un administrador puede programar mantenimientos");
        }
        Asset asset = assetRepository.findById(request.assetId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Activo no encontrado"));
        if (!asset.isActive()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se puede programar un activo inactivo");
        }
        Usuario technician = usuarioRepository.findById(request.technicianId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Técnico no encontrado"));
        if (Boolean.FALSE.equals(technician.getActivo())
                || !userRoleService.getEffectiveRoles(technician).contains("TECNICO")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selecciona un técnico activo");
        }

        PreventiveMaintenance maintenance = new PreventiveMaintenance();
        maintenance.setAsset(asset);
        maintenance.setTechnician(technician);
        maintenance.setScheduledAt(request.scheduledAt());
        maintenance.setAttentionIndex(request.attentionIndex() != null
                ? request.attentionIndex()
                : MaintenanceAttentionIndex.LOW);
        maintenance.setPredictiveContext(request.predictiveContext());
        PreventiveMaintenance saved = maintenanceRepository.save(maintenance);
        saveScheduledMaintenanceHistory(saved);
        return toResponse(saved);
    }

    @Transactional
    public PreventiveMaintenanceResponse execute(
            Long id,
            PreventiveMaintenanceExecutionRequest request,
            List<MultipartFile> evidenceFiles,
            Authentication authentication) {
        PreventiveMaintenance maintenance = requireVisibleMaintenance(id, authentication);
        if (isAdministrator(authentication)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "La ejecución debe registrarla el técnico asignado");
        }
        if (maintenance.getStatus() != PreventiveMaintenanceStatus.SCHEDULED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Este mantenimiento ya fue ejecutado");
        }
        if (!request.confirmed()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Confirma la veracidad de los datos para firmar");
        }
        if (request.nextMaintenanceDate() != null && request.nextMaintenanceDate().isBefore(LocalDate.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La próxima fecha debe ser hoy o una fecha futura");
        }

        LocalDateTime executedAt = LocalDateTime.now();
        LocalDate nextDate = request.nextMaintenanceDate() != null
                ? request.nextMaintenanceDate()
                : executedAt.toLocalDate().plusDays(maintenance.getAsset().getCategory().getMaintenanceFrequencyDays());

        maintenance.setExecutedBy(maintenance.getTechnician());
        maintenance.setExecutedAt(executedAt);
        maintenance.setStatus(toStatus(request.outcome()));
        maintenance.setReplacedParts(request.replacedParts());
        maintenance.setWorkDescription(request.workDescription());
        maintenance.setTechnicianNotes(request.technicianNotes());
        maintenance.setNextMaintenanceDate(nextDate);
        maintenance.setConfirmedAt(executedAt);

        for (PreventiveMaintenanceTaskRequest taskRequest : request.tasks()) {
            PreventiveMaintenanceTask task = new PreventiveMaintenanceTask();
            task.setMaintenance(maintenance);
            task.setDescription(taskRequest.description().trim());
            task.setCompleted(taskRequest.completed());
            maintenance.getTasks().add(task);
        }
        for (String url : evidenceStorageService.storeAll(evidenceFiles)) {
            PreventiveMaintenanceEvidence evidence = new PreventiveMaintenanceEvidence();
            evidence.setMaintenance(maintenance);
            evidence.setUrl(url);
            maintenance.getEvidences().add(evidence);
        }

        updateMaintenanceHistory(maintenance, request);
        return toResponse(maintenanceRepository.save(maintenance));
    }

    private void saveScheduledMaintenanceHistory(PreventiveMaintenance maintenance) {
        MaintenanceHistory history = new MaintenanceHistory();
        history.setAsset(maintenance.getAsset());
        history.setPreventiveMaintenance(maintenance);
        history.setType(MaintenanceType.PREVENTIVE);
        history.setStatus(MaintenanceHistoryStatus.SCHEDULED);
        history.setMaintenanceDate(maintenance.getScheduledAt().toLocalDate());
        history.setScheduledAt(maintenance.getScheduledAt());
        history.setTechnicianName(maintenance.getTechnician().getNombre());
        String historyDescription = "Mantenimiento preventivo programado para "
                + maintenance.getAsset().getName()
                + ".\nÍndice de atención: " + attentionLabel(effectiveAttentionIndex(maintenance)) + ".";
        if (maintenance.getPredictiveContext() != null && !maintenance.getPredictiveContext().isBlank()) {
            historyDescription += "\n\nContexto predictivo:\n" + maintenance.getPredictiveContext().trim();
        }
        history.setTasksDescription(historyDescription);
        maintenanceHistoryRepository.save(history);
    }

    private void updateMaintenanceHistory(
            PreventiveMaintenance maintenance, PreventiveMaintenanceExecutionRequest request) {
        String taskSummary = request.tasks().stream()
                .map(task -> (task.completed() ? "[x] " : "[ ] ") + task.description().trim())
                .collect(java.util.stream.Collectors.joining("\n"));
        String summary = """
                Tareas de inspección:
                %s
                Repuestos: %s
                Trabajo realizado: %s
                Observaciones: %s
                Índice de atención: %s
                Contexto predictivo:
                %s
                Resultado: %s
                """.formatted(
                taskSummary,
                valueOrEmpty(request.replacedParts()),
                valueOrEmpty(request.workDescription()),
                valueOrEmpty(request.technicianNotes()),
                attentionLabel(effectiveAttentionIndex(maintenance)),
                valueOrEmpty(maintenance.getPredictiveContext()),
                request.outcome());

        MaintenanceHistory history = maintenanceHistoryRepository
                .findByPreventiveMaintenanceId(maintenance.getId())
                .orElseGet(() -> {
                    MaintenanceHistory newHistory = new MaintenanceHistory();
                    newHistory.setAsset(maintenance.getAsset());
                    newHistory.setPreventiveMaintenance(maintenance);
                    return newHistory;
                });
        history.setType(MaintenanceType.PREVENTIVE);
        history.setStatus(toHistoryStatus(request.outcome()));
        history.setMaintenanceDate(maintenance.getExecutedAt().toLocalDate());
        history.setScheduledAt(maintenance.getScheduledAt());
        history.setExecutedAt(maintenance.getExecutedAt());
        history.setTechnicianName(maintenance.getExecutedBy().getNombre());
        history.setReplacedParts(maintenance.getReplacedParts());
        history.setSignedAt(maintenance.getConfirmedAt());
        history.setTasksDescription(summary);
        history.setNextRecommendedDate(maintenance.getNextMaintenanceDate());
        maintenanceHistoryRepository.save(history);
    }

    private MaintenanceHistoryStatus toHistoryStatus(MaintenanceOutcome outcome) {
        return switch (outcome) {
            case COMPLETED -> MaintenanceHistoryStatus.COMPLETED;
            case PARTIALLY_COMPLETED -> MaintenanceHistoryStatus.PARTIALLY_COMPLETED;
            case URGENT_CORRECTIVE -> MaintenanceHistoryStatus.URGENT_CORRECTIVE;
        };
    }

    private PreventiveMaintenance requireVisibleMaintenance(Long id, Authentication authentication) {
        PreventiveMaintenance maintenance = maintenanceRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Mantenimiento no encontrado"));
        if (!isAdministrator(authentication) && !maintenance.getTechnician().getEmail().equals(authentication.getName())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Este mantenimiento no está asignado a tu usuario");
        }
        return maintenance;
    }

    private boolean isAdministrator(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority -> authority.equals("ROLE_ADMIN") || authority.equals("ROLE_ADMINISTRADOR"));
    }

    private PreventiveMaintenanceStatus toStatus(MaintenanceOutcome outcome) {
        return switch (outcome) {
            case COMPLETED -> PreventiveMaintenanceStatus.COMPLETED;
            case PARTIALLY_COMPLETED -> PreventiveMaintenanceStatus.PARTIALLY_COMPLETED;
            case URGENT_CORRECTIVE -> PreventiveMaintenanceStatus.URGENT_CORRECTIVE;
        };
    }

    private String valueOrEmpty(String value) {
        return value == null || value.isBlank() ? "Ninguno" : value.trim();
    }

    private String attentionLabel(MaintenanceAttentionIndex attentionIndex) {
        return switch (attentionIndex) {
            case LOW -> "Baja";
            case MEDIUM -> "Media";
            case HIGH -> "Alta";
        };
    }

    private MaintenanceAttentionIndex effectiveAttentionIndex(PreventiveMaintenance maintenance) {
        return maintenance.getAttentionIndex() != null
                ? maintenance.getAttentionIndex()
                : MaintenanceAttentionIndex.LOW;
    }

    private PreventiveMaintenanceResponse toResponse(PreventiveMaintenance maintenance) {
        return new PreventiveMaintenanceResponse(
                maintenance.getId(),
                maintenance.getAsset().getId(),
                maintenance.getAsset().getName(),
                maintenance.getAsset().getCategory().getName(),
                maintenance.getAsset().getCategory().getMaintenanceFrequencyDays(),
                maintenance.getTechnician().getId(),
                maintenance.getTechnician().getNombre(),
                maintenance.getScheduledAt(),
                maintenance.getStatus(),
                maintenance.getExecutedAt(),
                maintenance.getReplacedParts(),
                maintenance.getWorkDescription(),
                maintenance.getTechnicianNotes(),
                maintenance.getNextMaintenanceDate(),
                maintenance.getConfirmedAt(),
                maintenance.getTasks().stream()
                        .map(task -> new PreventiveMaintenanceTaskResponse(
                                task.getId(), task.getDescription(), task.isCompleted()))
                        .toList(),
                maintenance.getEvidences().stream()
                        .map(PreventiveMaintenanceEvidence::getUrl)
                        .toList(),
                effectiveAttentionIndex(maintenance),
                maintenance.getPredictiveContext());
    }
}
