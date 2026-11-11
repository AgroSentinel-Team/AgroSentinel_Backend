package com.agrosentinel.agrosentinel.services;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.agrosentinel.agrosentinel.dtos.FailureAssetOption;
import com.agrosentinel.agrosentinel.dtos.FailureInterventionRequest;
import com.agrosentinel.agrosentinel.dtos.FailureReportRequest;
import com.agrosentinel.agrosentinel.dtos.FailureReportResponse;
import com.agrosentinel.agrosentinel.dtos.FailureReportCloseRequest;
import com.agrosentinel.agrosentinel.dtos.MaintenanceHistoryResponse;
import com.agrosentinel.agrosentinel.dtos.AssetResumeResponse;
import com.agrosentinel.agrosentinel.dtos.MaintenancePartRequest;
import com.agrosentinel.agrosentinel.models.Asset;
import com.agrosentinel.agrosentinel.models.EquipmentFinalState;
import com.agrosentinel.agrosentinel.models.FailurePriority;
import com.agrosentinel.agrosentinel.models.FailureReport;
import com.agrosentinel.agrosentinel.models.FailureStatus;
import com.agrosentinel.agrosentinel.models.InventoryItem;
import com.agrosentinel.agrosentinel.models.InventoryMovement;
import com.agrosentinel.agrosentinel.models.InventoryMovementType;
import com.agrosentinel.agrosentinel.repositories.InventoryItemRepository;
import com.agrosentinel.agrosentinel.repositories.InventoryMovementRepository;
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
import com.agrosentinel.agrosentinel.repositories.PreventiveMaintenanceRepository;
import com.agrosentinel.agrosentinel.repositories.UsuarioRepository;
import com.agrosentinel.agrosentinel.repositories.WorkGroupRepository;

import lombok.RequiredArgsConstructor;

@Service
@Transactional(readOnly = true)
public class FailureReportService {
    private static final long TECHNICIAN_WORKLOAD_CAPACITY = 3;
    private static final Set<FailureStatus> ACTIVE_WORKLOAD_STATUSES = Set.of(
            FailureStatus.PENDIENTE,
            FailureStatus.ASIGNADA,
            FailureStatus.EN_EJECUCION,
            FailureStatus.EN_CIERRE,
            FailureStatus.EN_REVISION,
            FailureStatus.ESPERA_REPUESTO);

    private final FailureReportRepository failureReportRepository;
    private final AssetRepository assetRepository;
    private final UsuarioRepository usuarioRepository;
    private final WorkGroupRepository workGroupRepository;
    private final MaintenanceHistoryRepository maintenanceHistoryRepository;
    private final FailureEvidenceStorageService evidenceStorageService;
    private final PreventiveMaintenanceEvidenceStorageService maintenanceEvidenceStorageService;
    private final UserRoleService userRoleService;
    private final AssetService assetService;
    private final InventoryItemRepository inventoryItemRepository;
    private final InventoryMovementRepository inventoryMovementRepository;
    private final UserNotificationService notificationService;
    private final PreventiveMaintenanceRepository preventiveMaintenanceRepository;

    @Autowired
    public FailureReportService(
            FailureReportRepository failureReportRepository,
            AssetRepository assetRepository,
            UsuarioRepository usuarioRepository,
            WorkGroupRepository workGroupRepository,
            MaintenanceHistoryRepository maintenanceHistoryRepository,
            FailureEvidenceStorageService evidenceStorageService,
            PreventiveMaintenanceEvidenceStorageService maintenanceEvidenceStorageService,
            UserRoleService userRoleService,
            AssetService assetService,
            InventoryItemRepository inventoryItemRepository,
            InventoryMovementRepository inventoryMovementRepository,
            UserNotificationService notificationService,
            PreventiveMaintenanceRepository preventiveMaintenanceRepository) {
        this.failureReportRepository = failureReportRepository;
        this.assetRepository = assetRepository;
        this.usuarioRepository = usuarioRepository;
        this.workGroupRepository = workGroupRepository;
        this.maintenanceHistoryRepository = maintenanceHistoryRepository;
        this.evidenceStorageService = evidenceStorageService;
        this.maintenanceEvidenceStorageService = maintenanceEvidenceStorageService;
        this.userRoleService = userRoleService;
        this.assetService = assetService;
        this.inventoryItemRepository = inventoryItemRepository;
        this.inventoryMovementRepository = inventoryMovementRepository;
        this.notificationService = notificationService;
        this.preventiveMaintenanceRepository = preventiveMaintenanceRepository;
    }

    public FailureReportService(
            FailureReportRepository failureReportRepository,
            AssetRepository assetRepository,
            UsuarioRepository usuarioRepository,
            WorkGroupRepository workGroupRepository,
            MaintenanceHistoryRepository maintenanceHistoryRepository,
            FailureEvidenceStorageService evidenceStorageService,
            UserRoleService userRoleService,
            AssetService assetService) {
        this(failureReportRepository, assetRepository, usuarioRepository, workGroupRepository,
                maintenanceHistoryRepository, evidenceStorageService, null, userRoleService, assetService, null, null, null, null);
    }

    public FailureReportService(
            FailureReportRepository failureReportRepository,
            AssetRepository assetRepository,
            UsuarioRepository usuarioRepository,
            WorkGroupRepository workGroupRepository,
            MaintenanceHistoryRepository maintenanceHistoryRepository,
            FailureEvidenceStorageService evidenceStorageService,
            UserRoleService userRoleService,
            AssetService assetService,
            InventoryItemRepository inventoryItemRepository,
            InventoryMovementRepository inventoryMovementRepository) {
        this(failureReportRepository, assetRepository, usuarioRepository, workGroupRepository,
                maintenanceHistoryRepository, evidenceStorageService, null, userRoleService, assetService,
                inventoryItemRepository, inventoryMovementRepository, null, null);
    }

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
        boolean canSelectTechnician = isAdministrator(authentication) || hasRole(reporter, "SUPERVISOR");
        if (!isAdministrator(authentication)
                && !hasRole(reporter, "TECNICO")
                && !hasRole(reporter, "SUPERVISOR")
                && !hasRole(reporter, "OPERATIVO_CAMPO")
                && !hasRole(reporter, "OPERARIO_CAMPO")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tu rol no puede reportar fallas");
        }
        WorkGroup group = resolveGroup(request.groupId(), reporter, authentication);
        if (request.assignedTechnicianId() != null && !canSelectTechnician) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Solo administradores y supervisores pueden elegir al técnico");
        }
        Usuario technician = request.assignedTechnicianId() == null
                ? chooseTechnician(group)
                : chooseTechnician(group, request.assignedTechnicianId());

        FailureReport report = new FailureReport();
        report.setAsset(asset);
        report.setUser(reporter);
        report.setGroup(group);
        report.setAssignedTechnician(technician);
        report.setTipoFalla(request.tipoFalla().trim());
        report.setPrioridad(request.prioridad());
        report.setDescripcion(request.descripcion().trim());
        report.setFechaReporte(LocalDateTime.now());
        report.setFechaAsignacion(LocalDateTime.now());
        report.setEstado(FailureStatus.ASIGNADA);
        report.setEvidenciaUrl(evidenceStorageService.store(evidence));

        if (request.prioridad() == FailurePriority.ALTA || request.prioridad() == FailurePriority.CRITICA) {
            asset.setOperationalStatus(OperationalStatus.IN_REPAIR);
        }

        FailureReport savedReport = failureReportRepository.save(report);
        if (notificationService != null) {
            notificationService.notifyAssignment(
                    technician,
                    "Nueva orden correctiva asignada",
                    "Falla F-" + savedReport.getId() + " del activo " + asset.getName() + ".",
                    "/tecnico/dashboard");
        }
        return toResponse(savedReport);
    }

    @Transactional
    public FailureReportResponse startReview(Long id, Authentication authentication) {
        FailureReport report = requireAssignedReport(id, authentication);
        if (report.getEstado() == FailureStatus.EN_EJECUCION) {
            return toResponse(report);
        }
        if (report.getEstado() != FailureStatus.ASIGNADA
                && report.getEstado() != FailureStatus.PENDIENTE
                && report.getEstado() != FailureStatus.EN_REVISION) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "La orden solo puede iniciar desde el estado Asignada");
        }
        LocalDateTime startedAt = LocalDateTime.now();
        report.setFechaInicioRevision(startedAt);
        report.setEstado(FailureStatus.EN_EJECUCION);
        report.getAsset().setOperationalStatus(OperationalStatus.IN_REPAIR);
        return toResponse(failureReportRepository.save(report));
    }

    @Transactional
    public FailureReportResponse saveIntervention(
            Long id,
            FailureInterventionRequest request,
            List<MultipartFile> evidences,
            Authentication authentication) {
        FailureReport report = requireAssignedReport(id, authentication);
        if (report.getEstado() != FailureStatus.EN_EJECUCION
                && report.getEstado() != FailureStatus.EN_REVISION) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "La intervención solo puede enviarse cuando la orden está En ejecución");
        }
        if (report.getEstado() == FailureStatus.EN_REVISION) {
            report.getAsset().setOperationalStatus(OperationalStatus.IN_REPAIR);
        }

        LocalDateTime now = LocalDateTime.now();
        if (report.getFechaInicioRevision() == null) {
            report.setFechaInicioRevision(now);
        }
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
        if (maintenanceEvidenceStorageService != null) {
            history.getEvidences().addAll(maintenanceEvidenceStorageService.storeAll(evidences));
        }
        history.setStatus(MaintenanceHistoryStatus.PENDING_REVIEW);
        report.setEstado(FailureStatus.EN_CIERRE);
        report.setFechaEnvioCierre(now);

        maintenanceHistoryRepository.save(history);
        return toResponse(failureReportRepository.save(report));
    }

    public FailureReportResponse saveIntervention(
            Long id, FailureInterventionRequest request, Authentication authentication) {
        return saveIntervention(id, request, List.of(), authentication);
    }

    public MaintenanceHistoryResponse getIntervention(Long id, Authentication authentication) {
        FailureReport report = requireVisibleReport(id, authentication);
        MaintenanceHistory history = maintenanceHistoryRepository.findByFailureReportId(report.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "El técnico aún no ha enviado la intervención para revisión"));
        return assetService.toHistoryResponse(history);
    }

    @Transactional
    public FailureReportResponse closeAfterReview(
            Long id, FailureReportCloseRequest request, Authentication authentication) {
        if (!isAdministrator(authentication)
                && !hasRole(requireActiveUser(authentication.getName()), "SUPERVISOR")) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Solo el administrador o supervisor puede cerrar la orden");
        }
        FailureReport report = failureReportRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Falla no encontrada"));
        if (report.getEstado() != FailureStatus.EN_CIERRE) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "La orden debe estar En cierre y tener una intervención para revisión");
        }
        MaintenanceHistory history = maintenanceHistoryRepository.findByFailureReportId(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.CONFLICT, "No hay una intervención técnica para cerrar"));
        if (history.getFinalEquipmentState() == null) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "La intervención no tiene un estado final del activo");
        }
        LocalDateTime closedAt = LocalDateTime.now();
        Usuario reviewer = requireActiveUser(authentication.getName());
        history.setReviewObservations(request.reviewObservations().trim());
        history.setReviewedBy(reviewer.getNombre());
        history.setReviewedAt(closedAt);
        history.setStatus(history.getFinalEquipmentState() == EquipmentFinalState.INOPERATIVE_WAITING_PART
                ? MaintenanceHistoryStatus.PARTIALLY_COMPLETED
                : MaintenanceHistoryStatus.COMPLETED);
        report.setEstado(FailureStatus.RESUELTO);
        report.setFechaCierre(closedAt);
        report.getAsset().setOperationalStatus(switch (history.getFinalEquipmentState()) {
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
        updateHorometer(report.getAsset(), history, request.horometerReading());
        history.setTestsPerformed(request.testsPerformed().trim());
        history.setFinalEquipmentState(request.finalEquipmentState());
        history.setRecommendations(request.recommendations().trim());
        history.setPendingTasks(normalizeOptional(request.pendingTasks()));
        history.setTechnicianName(request.technicianName().trim());
        history.setReceivedBy(normalizeOptional(request.receivedBy()));
        List<MaintenancePartRequest> resolvedParts = new ArrayList<>();
        if (request.replacedParts() != null && !request.replacedParts().isEmpty()) {
            for (MaintenancePartRequest partReq : request.replacedParts()) {
                if (partReq.itemId() != null && inventoryItemRepository != null) {
                    InventoryItem item = inventoryItemRepository.findByIdForUpdate(partReq.itemId())
                            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                    "Repuesto no encontrado en inventario (ID: " + partReq.itemId() + ")"));
                    Long itemGroupId = item.getWorkGroup() == null ? null : item.getWorkGroup().getId();
                    if (!Objects.equals(itemGroupId, partReq.sourceGroupId())) {
                        throw new ResponseStatusException(
                                HttpStatus.BAD_REQUEST, "El repuesto no pertenece a la bodega seleccionada");
                    }
                    String itemOrigin = item.getWorkGroup() == null
                            ? "Bodega Central"
                            : item.getWorkGroup().getName();
                    java.math.BigDecimal available = item.getStock() == null ? java.math.BigDecimal.ZERO : item.getStock();
                    if (partReq.quantity().compareTo(available) > 0) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                "Stock insuficiente para el repuesto \"" + item.getName() +
                                "\". Disponible: " + available + " " + item.getUnit() +
                                ", Requerido: " + partReq.quantity() + " " + item.getUnit());
                    }
                    item.setStock(available.subtract(partReq.quantity()));
                    inventoryItemRepository.save(item);

                    if (inventoryMovementRepository != null) {
                        InventoryMovement movement = new InventoryMovement();
                        movement.setItem(item);
                        movement.setTipo(InventoryMovementType.SALIDA);
                        movement.setCantidad(partReq.quantity());
                        movement.setResponsable(request.technicianName().trim());
                        movement.setRol("tecnico");
                        String origin = item.getWorkGroup() != null
                                ? " (" + item.getWorkGroup().getName() + ")"
                                : " (Bodega Central)";
                        movement.setMotivo("Uso en intervención técnica de falla F-" + report.getId() + " - " + report.getAsset().getName() + origin);

                        inventoryMovementRepository.save(movement);
                    }
                    resolvedParts.add(new MaintenancePartRequest(
                            item.getCategory(),
                            item.getName(),
                            partReq.brand(),
                            partReq.quantity(),
                            item.getId(),
                            itemGroupId,
                            itemOrigin));
                } else {
                    resolvedParts.add(partReq);
                }
            }
        }
        List<MaintenancePartRequest> replacedParts = resolvedParts;
        history.setParts(replacedParts.stream()
                .map(this::toMaintenancePart)
                .collect(Collectors.toCollection(java.util.ArrayList::new)));
        history.setReplacedParts(replacedParts.stream()
                .map(part -> {
                    String origin = part.sourceZoneName() == null || part.sourceZoneName().isBlank()
                            ? ""
                            : " [" + part.sourceZoneName().trim() + "]";
                    String brandStr = part.brand() != null && !part.brand().isBlank()
                            ? " (" + part.brand().trim() + ")"
                            : "";
                    String partNum = part.partNumber() != null ? part.partNumber().trim() : "REP-" + part.itemId();
                    String partNm = part.name() != null ? part.name().trim() : "Repuesto";
                    String qtyStr = part.quantity() != null ? part.quantity().stripTrailingZeros().toPlainString() : "1";
                    return partNum + " - " + partNm
                            + brandStr + origin
                            + " x " + qtyStr;
                })
                .collect(Collectors.joining("; ")));
        history.setSignedAt(executedAt);
    }

    private void updateHorometer(Asset asset, MaintenanceHistory history, java.math.BigDecimal reading) {
        if (reading == null) return;
        Asset lockedAsset = assetRepository.findByIdForUpdate(asset.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Activo no encontrado"));
        java.math.BigDecimal current = lockedAsset.getCurrentHorometer() == null
                ? java.math.BigDecimal.ZERO
                : lockedAsset.getCurrentHorometer();
        if (reading.compareTo(current) < 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "El horómetro no puede ser menor a la última lectura registrada (" + current + ")");
        }
        lockedAsset.setCurrentHorometer(reading);
        history.setHorometerAtMaintenance(reading);
    }

    private MaintenancePart toMaintenancePart(MaintenancePartRequest request) {
        MaintenancePart part = new MaintenancePart();
        part.setPartNumber(request.partNumber() != null ? request.partNumber().trim() : "REP-" + request.itemId());
        part.setName(request.name() != null ? request.name().trim() : "Repuesto");
        part.setBrand(normalizeOptional(request.brand()));
        part.setQuantity(request.quantity() != null ? request.quantity() : java.math.BigDecimal.ONE);
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
                .map(this::lockTechnician)
                .filter(user -> activeWorkload(user) < TECHNICIAN_WORKLOAD_CAPACITY)
                .sorted(Comparator.comparingLong(this::activeWorkload).thenComparing(Usuario::getId))
                .toList();
        if (technicians.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "No hay técnicos disponibles en el grupo (máximo 3 reportes activos por técnico)");
        }

        Usuario selected = technicians.getFirst();
        group.setLastAssignedTechnicianId(selected.getId());
        workGroupRepository.save(group);
        return selected;
    }

    private Usuario chooseTechnician(WorkGroup group, Long requestedTechnicianId) {
        Usuario candidate = group.getMembers().stream()
                .filter(user -> user.getId().equals(requestedTechnicianId))
                .filter(user -> !Boolean.FALSE.equals(user.getActivo()) && hasRole(user, "TECNICO"))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "El técnico seleccionado debe estar activo y pertenecer al grupo con rol de técnico"));
        Usuario technician = lockTechnician(candidate);
        if (activeWorkload(technician) >= TECHNICIAN_WORKLOAD_CAPACITY) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "El técnico seleccionado alcanzó su máximo de 3 trabajos activos");
        }
        return technician;
    }

    private Usuario lockTechnician(Usuario technician) {
        return usuarioRepository.findByIdForUpdate(technician.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Técnico no encontrado"));
    }

    private long activeWorkload(Usuario technician) {
        long active = failureReportRepository.countByAssignedTechnicianIdAndEstadoIn(
                technician.getId(), ACTIVE_WORKLOAD_STATUSES);
        if (preventiveMaintenanceRepository != null) {
            active += preventiveMaintenanceRepository.countByTechnician_IdAndStatusIn(
                    technician.getId(), Set.of(
                            com.agrosentinel.agrosentinel.models.PreventiveMaintenanceStatus.SCHEDULED,
                            com.agrosentinel.agrosentinel.models.PreventiveMaintenanceStatus.PARTIALLY_COMPLETED));
        }
        return active;
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
        FailureReport report = failureReportRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Falla no encontrada"));
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
                report.getAsset().getInventoryCode(),
                report.getFechaAsignacion(),
                report.getFechaCierre(),
                report.getFechaEnvioCierre(),
                report.getFechaInicioRevision() == null || report.getFechaCierre() == null
                        ? null
                        : Duration.between(report.getFechaInicioRevision(), report.getFechaCierre()).toMinutes());
    }
}
