package com.agrosentinel.agrosentinel.services;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.agrosentinel.agrosentinel.dtos.FailureAssetOption;
import com.agrosentinel.agrosentinel.dtos.FailureInterventionRequest;
import com.agrosentinel.agrosentinel.dtos.FailureReportRequest;
import com.agrosentinel.agrosentinel.dtos.FailureReportResponse;
import com.agrosentinel.agrosentinel.dtos.AssetResumeResponse;
import com.agrosentinel.agrosentinel.dtos.MaintenancePartRequest;
import com.agrosentinel.agrosentinel.models.Asset;
import com.agrosentinel.agrosentinel.models.EquipmentFinalState;
import com.agrosentinel.agrosentinel.models.FailurePriority;
import com.agrosentinel.agrosentinel.models.FailureReport;
import com.agrosentinel.agrosentinel.models.FailureStatus;
import com.agrosentinel.agrosentinel.models.MaintenanceHistory;
import com.agrosentinel.agrosentinel.models.MaintenanceHistoryStatus;
import com.agrosentinel.agrosentinel.models.MaintenancePart;
import com.agrosentinel.agrosentinel.models.MaintenanceType;
import com.agrosentinel.agrosentinel.models.OperationalStatus;
import com.agrosentinel.agrosentinel.models.Usuario;
import com.agrosentinel.agrosentinel.models.WorkGroup;
import com.agrosentinel.agrosentinel.repositories.AssetRepository;
import com.agrosentinel.agrosentinel.repositories.FailureReportRepository;
import com.agrosentinel.agrosentinel.repositories.MaintenanceHistoryRepository;
import com.agrosentinel.agrosentinel.repositories.UsuarioRepository;
import com.agrosentinel.agrosentinel.repositories.WorkGroupRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FailureReportService {
    private final FailureReportRepository failureReportRepository;
    private final AssetRepository assetRepository;
    private final UsuarioRepository usuarioRepository;
    private final WorkGroupRepository workGroupRepository;
    private final MaintenanceHistoryRepository maintenanceHistoryRepository;
    private final FailureEvidenceStorageService evidenceStorageService;
    private final UserRoleService userRoleService;
    private final AssetService assetService;

    public List<FailureReportResponse> findAll(Authentication authentication) {
        Usuario currentUser = requireActiveUser(authentication.getName());
        if (isAdministrator(authentication) || hasRole(currentUser, "SUPERVISOR")) {
            return failureReportRepository.findAllByOrderByFechaReporteDesc().stream()
                    .map(this::toResponse)
                    .toList();
        }

        Map<Long, FailureReport> visibleReports = new LinkedHashMap<>();
        failureReportRepository.findAllByUserIdOrderByFechaReporteDesc(currentUser.getId())
                .forEach(report -> visibleReports.put(report.getId(), report));
        failureReportRepository.findAllByAssignedTechnicianIdOrderByFechaReporteDesc(currentUser.getId())
                .forEach(report -> visibleReports.put(report.getId(), report));
        return visibleReports.values().stream()
                .sorted(Comparator.comparing(FailureReport::getFechaReporte).reversed())
                .map(this::toResponse)
                .toList();
    }

    public List<FailureReportResponse> findAssignedToTechnician(Authentication authentication) {
        Usuario technician = requireActiveUser(authentication.getName());
        if (!hasRole(technician, "TECNICO")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Esta consulta es exclusiva para técnicos");
        }
        return failureReportRepository.findAllByAssignedTechnicianIdOrderByFechaReporteDesc(technician.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public FailureReportResponse findById(Long id, Authentication authentication) {
        return toResponse(requireVisibleReport(id, authentication));
    }

    public AssetResumeResponse getAssignedAssetResume(Long id, Authentication authentication) {
        FailureReport report = requireVisibleReport(id, authentication);
        if (!isAdministrator(authentication)) {
            Usuario currentUser = requireActiveUser(authentication.getName());
            if (report.getAssignedTechnician() == null
                    || !report.getAssignedTechnician().getId().equals(currentUser.getId())) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "Solo el técnico asignado puede consultar la hoja de vida desde esta falla");
            }
        }
        return assetService.getResume(report.getAsset().getId());
    }

    public List<FailureAssetOption> findAssets(String currentUserEmail) {
        return assetRepository.findAll().stream()
                .filter(Asset::isActive)
                .map(asset -> new FailureAssetOption(
                        asset.getId(), asset.getName(), asset.getBrand(), asset.getModel(), asset.getSerialNumber(),
                        asset.getInventoryCode(), asset.getLocation(), asset.getResponsible().getId(),
                        asset.getResponsible().getNombre(),
                        asset.getOperationalStatus(), currentUserEmail.equalsIgnoreCase(asset.getResponsible().getEmail())))
                .toList();
    }

    @Transactional
    public FailureReportResponse create(
            FailureReportRequest request, MultipartFile evidence, Authentication authentication) {
        Asset asset = assetRepository.findById(request.assetId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Activo no encontrado"));
        if (!asset.isActive()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se puede reportar un activo inactivo");
        }
        Usuario reporter = requireActiveUser(authentication.getName());
        if (!isAdministrator(authentication)
                && !hasRole(reporter, "TECNICO")
                && !hasRole(reporter, "OPERATIVO_CAMPO")
                && !hasRole(reporter, "OPERARIO_CAMPO")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tu rol no puede reportar fallas");
        }
        WorkGroup group = resolveGroup(request.groupId(), reporter, authentication);
        Usuario technician = chooseTechnician(group);

        FailureReport report = new FailureReport();
        report.setAsset(asset);
        report.setUser(reporter);
        report.setGroup(group);
        report.setAssignedTechnician(technician);
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

    @Transactional
    public FailureReportResponse startReview(Long id, Authentication authentication) {
        FailureReport report = requireAssignedReport(id, authentication);
        if (report.getEstado() == FailureStatus.RESUELTO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La falla ya fue resuelta");
        }
        if (report.getFechaInicioRevision() == null) {
            report.setFechaInicioRevision(LocalDateTime.now());
        }
        report.setEstado(FailureStatus.EN_REVISION);
        return toResponse(failureReportRepository.save(report));
    }

    @Transactional
    public FailureReportResponse saveIntervention(
            Long id, FailureInterventionRequest request, Authentication authentication) {
        FailureReport report = requireAssignedReport(id, authentication);
        if (report.getEstado() == FailureStatus.RESUELTO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La falla ya fue resuelta");
        }

        LocalDateTime now = LocalDateTime.now();
        if (report.getFechaInicioRevision() == null) {
            report.setFechaInicioRevision(now);
        }
        report.setEstado(FailureStatus.EN_REVISION);

        MaintenanceHistory history = maintenanceHistoryRepository.findByFailureReportId(id)
                .orElseGet(() -> {
                    MaintenanceHistory newHistory = new MaintenanceHistory();
                    newHistory.setAsset(report.getAsset());
                    newHistory.setFailureReport(report);
                    newHistory.setType(MaintenanceType.CORRECTIVE);
                    newHistory.setScheduledAt(report.getFechaReporte());
                    return newHistory;
                });
        applyIntervention(history, report, request, now);
        report.setEstado(request.finalEquipmentState() == EquipmentFinalState.INOPERATIVE_WAITING_PART
                ? FailureStatus.ESPERA_REPUESTO
                : FailureStatus.RESUELTO);
        report.getAsset().setOperationalStatus(switch (request.finalEquipmentState()) {
            case OPERATIONAL -> OperationalStatus.OPERATIONAL;
            case OPERATIONAL_WITH_RESTRICTIONS -> OperationalStatus.OPERATIONAL_WITH_RESTRICTIONS;
            case INOPERATIVE_WAITING_PART -> OperationalStatus.IN_REPAIR;
        });

        maintenanceHistoryRepository.save(history);
        return toResponse(failureReportRepository.save(report));
    }

    private void applyIntervention(
            MaintenanceHistory history,
            FailureReport report,
            FailureInterventionRequest request,
            LocalDateTime executedAt) {
        if (request.reportedAt().isAfter(executedAt)
                || request.inspectionStartedAt().isAfter(executedAt)
                || request.inspectionStartedAt().isBefore(request.reportedAt())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Las fechas deben ser válidas y la revisión no puede iniciar antes del reporte");
        }
        history.setAsset(report.getAsset());
        history.setFailureReport(report);
        history.setType(MaintenanceType.CORRECTIVE);
        history.setMaintenanceDate(request.inspectionStartedAt().toLocalDate());
        history.setExecutedAt(executedAt);
        history.setInspectionStartedAt(request.inspectionStartedAt());
        history.setEquipmentIntervened(request.equipmentIntervened().trim());
        history.setInitialReportedSymptom(request.initialReportedSymptom().trim());
        history.setInterventionReportedAt(request.reportedAt());
        history.setActualFailure(request.actualFailure().trim());
        history.setRootCause(request.rootCause().trim());
        history.setTasksDescription(request.tasksPerformed().trim());
        history.setWorkMinutes(request.workMinutes());
        history.setTestsPerformed(request.testsPerformed().trim());
        history.setFinalEquipmentState(request.finalEquipmentState());
        history.setRecommendations(request.recommendations().trim());
        history.setPendingTasks(normalizeOptional(request.pendingTasks()));
        history.setTechnicianName(request.technicianName().trim());
        history.setReceivedBy(normalizeOptional(request.receivedBy()));
        history.setStatus(request.finalEquipmentState() == EquipmentFinalState.INOPERATIVE_WAITING_PART
                ? MaintenanceHistoryStatus.PARTIALLY_COMPLETED
                : MaintenanceHistoryStatus.COMPLETED);
        history.setParts(request.replacedParts().stream()
                .map(this::toMaintenancePart)
                .collect(Collectors.toCollection(java.util.ArrayList::new)));
        history.setReplacedParts(request.replacedParts().stream()
                .map(part -> part.partNumber().trim() + " - " + part.name().trim()
                        + (part.brand() == null || part.brand().isBlank() ? "" : " (" + part.brand().trim() + ")")
                        + " x " + part.quantity().stripTrailingZeros().toPlainString())
                .collect(Collectors.joining("; ")));
        history.setSignedAt(executedAt);
    }

    private MaintenancePart toMaintenancePart(MaintenancePartRequest request) {
        MaintenancePart part = new MaintenancePart();
        part.setPartNumber(request.partNumber().trim());
        part.setName(request.name().trim());
        part.setBrand(normalizeOptional(request.brand()));
        part.setQuantity(request.quantity());
        return part;
    }

    private WorkGroup resolveGroup(Long requestedGroupId, Usuario reporter, Authentication authentication) {
        if (isAdministrator(authentication) && requestedGroupId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El administrador debe seleccionar un grupo");
        }
        if (requestedGroupId == null) {
            List<WorkGroup> memberGroups = workGroupRepository
                    .findAllByMembers_IdAndDeletedAtIsNullOrderByNameAsc(reporter.getId());
            if (memberGroups.size() != 1) {
                String message = memberGroups.isEmpty()
                        ? "El usuario no pertenece a ningún grupo activo"
                        : "Selecciona el grupo o zona a la que corresponde la falla";
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
            }
            return workGroupRepository.findActiveByIdForUpdate(memberGroups.getFirst().getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Grupo no encontrado"));
        }

        WorkGroup group = workGroupRepository.findActiveByIdForUpdate(requestedGroupId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Grupo no encontrado"));
        if (!isAdministrator(authentication)
                && group.getMembers().stream().noneMatch(member -> member.getId().equals(reporter.getId()))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo puedes reportar fallas en tus grupos");
        }
        return group;
    }

    private Usuario chooseTechnician(WorkGroup group) {
        List<Usuario> technicians = group.getMembers().stream()
                .filter(user -> !Boolean.FALSE.equals(user.getActivo()) && hasRole(user, "TECNICO"))
                .sorted(Comparator.comparing(Usuario::getId))
                .toList();
        if (technicians.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "El grupo no tiene miembros activos con permisos de técnico");
        }

        Long lastAssignedId = group.getLastAssignedTechnicianId();
        int lastIndex = -1;
        for (int index = 0; index < technicians.size(); index++) {
            if (technicians.get(index).getId().equals(lastAssignedId)) {
                lastIndex = index;
                break;
            }
        }
        Usuario selected = technicians.get((lastIndex + 1) % technicians.size());
        group.setLastAssignedTechnicianId(selected.getId());
        workGroupRepository.save(group);
        return selected;
    }

    private FailureReport requireVisibleReport(Long id, Authentication authentication) {
        FailureReport report = failureReportRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Falla no encontrada"));
        if (isAdministrator(authentication)) {
            return report;
        }
        Usuario currentUser = requireActiveUser(authentication.getName());
        if (hasRole(currentUser, "SUPERVISOR")) {
            return report;
        }
        boolean isReporter = report.getUser().getId().equals(currentUser.getId());
        boolean isAssignedTechnician = report.getAssignedTechnician() != null
                && report.getAssignedTechnician().getId().equals(currentUser.getId());
        if (!isReporter && !isAssignedTechnician) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes acceso a esta falla");
        }
        return report;
    }

    private FailureReport requireAssignedReport(Long id, Authentication authentication) {
        FailureReport report = requireVisibleReport(id, authentication);
        Usuario currentUser = requireActiveUser(authentication.getName());
        if (report.getAssignedTechnician() == null
                || !report.getAssignedTechnician().getId().equals(currentUser.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo el técnico asignado puede gestionar la falla");
        }
        return report;
    }

    private Usuario requireActiveUser(String email) {
        Usuario user = usuarioRepository.findByEmail(email);
        if (user == null || Boolean.FALSE.equals(user.getActivo())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario autenticado no disponible");
        }
        return user;
    }

    private boolean hasRole(Usuario user, String expectedRole) {
        return userRoleService.getEffectiveRoles(user).stream()
                .map(role -> role.toUpperCase(Locale.ROOT).replaceFirst("^ROLE_", ""))
                .anyMatch(role -> role.equals(expectedRole));
    }

    private boolean isAdministrator(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority -> authority.equals("ROLE_ADMINISTRADOR") || authority.equals("ROLE_ADMIN"));
    }

    private String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private FailureReportResponse toResponse(FailureReport report) {
        return new FailureReportResponse(
                report.getId(),
                report.getAsset().getId(),
                report.getAsset().getName(),
                report.getUser().getId(),
                report.getUser().getNombre(),
                report.getTipoFalla(),
                report.getPrioridad(),
                report.getDescripcion(),
                report.getFechaReporte(),
                report.getEstado(),
                report.getEvidenciaUrl(),
                report.getGroup() == null ? null : report.getGroup().getId(),
                report.getGroup() == null ? null : report.getGroup().getName(),
                report.getAssignedTechnician() == null ? null : report.getAssignedTechnician().getId(),
                report.getAssignedTechnician() == null ? null : report.getAssignedTechnician().getNombre(),
                report.getFechaInicioRevision(),
                report.getAsset().getBrand(),
                report.getAsset().getModel(),
                report.getAsset().getSerialNumber(),
                report.getAsset().getInventoryCode());
    }
}
