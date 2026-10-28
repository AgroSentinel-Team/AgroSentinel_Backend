package com.agrosentinel.agrosentinel.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import com.agrosentinel.agrosentinel.dtos.FailureInterventionRequest;
import com.agrosentinel.agrosentinel.dtos.FailureReportRequest;
import com.agrosentinel.agrosentinel.dtos.MaintenancePartRequest;
import com.agrosentinel.agrosentinel.models.Asset;
import com.agrosentinel.agrosentinel.models.EquipmentFinalState;
import com.agrosentinel.agrosentinel.models.FailureReport;
import com.agrosentinel.agrosentinel.models.FailurePriority;
import com.agrosentinel.agrosentinel.models.FailureStatus;
import com.agrosentinel.agrosentinel.models.MaintenanceHistory;
import com.agrosentinel.agrosentinel.models.OperationalStatus;
import com.agrosentinel.agrosentinel.models.Usuario;
import com.agrosentinel.agrosentinel.models.WorkGroup;
import com.agrosentinel.agrosentinel.repositories.AssetRepository;
import com.agrosentinel.agrosentinel.repositories.FailureReportRepository;
import com.agrosentinel.agrosentinel.repositories.MaintenanceHistoryRepository;
import com.agrosentinel.agrosentinel.repositories.UsuarioRepository;
import com.agrosentinel.agrosentinel.repositories.WorkGroupRepository;
import org.springframework.security.core.Authentication;

class FailureReportServiceTest {
    @Test
    void supervisorCanReadAllFailureReports() {
        FailureReportRepository reportRepository = mock(FailureReportRepository.class);
        UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
        UserRoleService userRoleService = mock(UserRoleService.class);
        Usuario supervisor = user(12L, "supervisor@example.com", "SUPERVISOR");
        when(usuarioRepository.findByEmail(supervisor.getEmail())).thenReturn(supervisor);
        when(userRoleService.getEffectiveRoles(supervisor)).thenReturn(Set.of("SUPERVISOR"));
        when(reportRepository.findAllByOrderByFechaReporteDesc()).thenReturn(List.of());

        FailureReportService service = new FailureReportService(
                reportRepository,
                mock(AssetRepository.class),
                usuarioRepository,
                mock(WorkGroupRepository.class),
                mock(MaintenanceHistoryRepository.class),
                mock(FailureEvidenceStorageService.class),
                userRoleService,
                mock(AssetService.class));

        assertEquals(List.of(), service.findAll(authentication(supervisor.getEmail(), "ROLE_SUPERVISOR")));
        verify(reportRepository).findAllByOrderByFechaReporteDesc();
    }

    @Test
    void onlyHighAndCriticalPriorityMoveAssetToRepair() {
        FailureReportRepository reportRepository = mock(FailureReportRepository.class);
        AssetRepository assetRepository = mock(AssetRepository.class);
        UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
        WorkGroupRepository workGroupRepository = mock(WorkGroupRepository.class);
        MaintenanceHistoryRepository historyRepository = mock(MaintenanceHistoryRepository.class);
        UserRoleService userRoleService = mock(UserRoleService.class);
        FailureEvidenceStorageService evidenceStorageService = mock(FailureEvidenceStorageService.class);
        Asset asset = new Asset();
        asset.setId(7L);
        asset.setName("Tractor de prueba");
        Usuario user = new Usuario();
        user.setId(3L);
        user.setEmail("operario@example.com");
        user.setNombre("Operario");
        user.setRol("OPERATIVO_CAMPO");
        WorkGroup group = new WorkGroup();
        group.setId(5L);
        group.setMembers(Set.of(user));
        group.setGrantedRoles(Set.of("TECNICO"));

        when(assetRepository.findById(anyLong())).thenReturn(Optional.of(asset));
        when(usuarioRepository.findByEmail(user.getEmail())).thenReturn(user);
        when(workGroupRepository.findActiveByIdForUpdate(5L)).thenReturn(Optional.of(group));
        when(userRoleService.getEffectiveRoles(user)).thenReturn(Set.of("OPERATIVO_CAMPO", "TECNICO"));
        when(reportRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        FailureReportService service = new FailureReportService(
                reportRepository, assetRepository, usuarioRepository, workGroupRepository, historyRepository,
                evidenceStorageService, userRoleService, mock(AssetService.class));

        service.create(request(FailurePriority.ALTA), null, authentication(user.getEmail(), "ROLE_OPERATIVO_CAMPO"));
        assertEquals(OperationalStatus.IN_REPAIR, asset.getOperationalStatus());

        asset.setOperationalStatus(OperationalStatus.OPERATIONAL);
        service.create(request(FailurePriority.CRITICA), null, authentication(user.getEmail(), "ROLE_OPERATIVO_CAMPO"));
        assertEquals(OperationalStatus.IN_REPAIR, asset.getOperationalStatus());

        asset.setOperationalStatus(OperationalStatus.OPERATIONAL);
        service.create(request(FailurePriority.MEDIA), null, authentication(user.getEmail(), "ROLE_OPERATIVO_CAMPO"));
        assertEquals(OperationalStatus.OPERATIONAL, asset.getOperationalStatus());
    }

    @Test
    void assignmentsRotateBetweenActiveTechniciansInGroup() {
        FailureReportRepository reportRepository = mock(FailureReportRepository.class);
        AssetRepository assetRepository = mock(AssetRepository.class);
        UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
        WorkGroupRepository workGroupRepository = mock(WorkGroupRepository.class);
        MaintenanceHistoryRepository historyRepository = mock(MaintenanceHistoryRepository.class);
        UserRoleService userRoleService = mock(UserRoleService.class);
        FailureEvidenceStorageService evidenceStorageService = mock(FailureEvidenceStorageService.class);
        Asset asset = new Asset();
        asset.setId(7L);
        asset.setName("Tractor");
        Usuario reporter = user(1L, "operario@example.com", "OPERATIVO_CAMPO");
        Usuario firstTechnician = user(2L, "tecnico1@example.com", "TECNICO");
        Usuario secondTechnician = user(3L, "tecnico2@example.com", "TECNICO");
        WorkGroup group = new WorkGroup();
        group.setId(5L);
        group.setMembers(Set.of(reporter, firstTechnician, secondTechnician));
        group.setGrantedRoles(Set.of());
        when(assetRepository.findById(7L)).thenReturn(Optional.of(asset));
        when(usuarioRepository.findByEmail(reporter.getEmail())).thenReturn(reporter);
        when(workGroupRepository.findActiveByIdForUpdate(5L)).thenReturn(Optional.of(group));
        when(userRoleService.getEffectiveRoles(reporter)).thenReturn(Set.of("OPERATIVO_CAMPO"));
        when(userRoleService.getEffectiveRoles(firstTechnician)).thenReturn(Set.of("TECNICO"));
        when(userRoleService.getEffectiveRoles(secondTechnician)).thenReturn(Set.of("TECNICO"));
        when(reportRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        FailureReportService service = new FailureReportService(
                reportRepository, assetRepository, usuarioRepository, workGroupRepository, historyRepository,
                evidenceStorageService, userRoleService, mock(AssetService.class));

        var first = service.create(request(FailurePriority.MEDIA), null,
                authentication(reporter.getEmail(), "ROLE_OPERATIVO_CAMPO"));
        var second = service.create(request(FailurePriority.MEDIA), null,
                authentication(reporter.getEmail(), "ROLE_OPERATIVO_CAMPO"));

        assertEquals(2L, first.assignedTechnicianId());
        assertEquals(3L, second.assignedTechnicianId());
    }

    @Test
    void supervisorCanSelectAnActiveTechnicianFromTheirGroup() {
        FailureReportRepository reportRepository = mock(FailureReportRepository.class);
        AssetRepository assetRepository = mock(AssetRepository.class);
        UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
        WorkGroupRepository workGroupRepository = mock(WorkGroupRepository.class);
        UserRoleService userRoleService = mock(UserRoleService.class);
        FailureEvidenceStorageService evidenceStorageService = mock(FailureEvidenceStorageService.class);
        Asset asset = new Asset();
        asset.setId(7L);
        asset.setName("Tractor");
        Usuario supervisor = user(1L, "supervisor@example.com", "SUPERVISOR");
        Usuario technician = user(2L, "tecnico@example.com", "TECNICO");
        WorkGroup group = new WorkGroup();
        group.setId(5L);
        group.setMembers(Set.of(supervisor, technician));

        when(assetRepository.findById(7L)).thenReturn(Optional.of(asset));
        when(usuarioRepository.findByEmail(supervisor.getEmail())).thenReturn(supervisor);
        when(workGroupRepository.findActiveByIdForUpdate(5L)).thenReturn(Optional.of(group));
        when(userRoleService.getEffectiveRoles(supervisor)).thenReturn(Set.of("SUPERVISOR"));
        when(userRoleService.getEffectiveRoles(technician)).thenReturn(Set.of("TECNICO"));
        when(reportRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        FailureReportService service = new FailureReportService(
                reportRepository, assetRepository, usuarioRepository, workGroupRepository,
                mock(MaintenanceHistoryRepository.class), evidenceStorageService, userRoleService,
                mock(AssetService.class));

        var response = service.create(
                new FailureReportRequest(
                        7L, "Falla mecánica", FailurePriority.MEDIA, "Descripción de prueba", 5L, 2L),
                null,
                authentication(supervisor.getEmail(), "ROLE_SUPERVISOR"));

        assertEquals(2L, response.assignedTechnicianId());
    }

    @Test
    void manuallySelectedTechnicianMustBelongToTheSelectedGroup() {
        FailureReportRepository reportRepository = mock(FailureReportRepository.class);
        AssetRepository assetRepository = mock(AssetRepository.class);
        UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
        WorkGroupRepository workGroupRepository = mock(WorkGroupRepository.class);
        UserRoleService userRoleService = mock(UserRoleService.class);
        Usuario supervisor = user(1L, "supervisor@example.com", "SUPERVISOR");
        WorkGroup group = new WorkGroup();
        group.setId(5L);
        group.setMembers(Set.of(supervisor));

        when(assetRepository.findById(7L)).thenReturn(Optional.of(new Asset()));
        when(usuarioRepository.findByEmail(supervisor.getEmail())).thenReturn(supervisor);
        when(workGroupRepository.findActiveByIdForUpdate(5L)).thenReturn(Optional.of(group));
        when(userRoleService.getEffectiveRoles(supervisor)).thenReturn(Set.of("SUPERVISOR"));

        FailureReportService service = new FailureReportService(
                reportRepository, assetRepository, usuarioRepository, workGroupRepository,
                mock(MaintenanceHistoryRepository.class), mock(FailureEvidenceStorageService.class),
                userRoleService, mock(AssetService.class));

        var exception = org.junit.jupiter.api.Assertions.assertThrows(
                org.springframework.web.server.ResponseStatusException.class,
                () -> service.create(
                        new FailureReportRequest(
                                7L, "Falla mecánica", FailurePriority.MEDIA, "Descripción de prueba", 5L, 2L),
                        null,
                        authentication(supervisor.getEmail(), "ROLE_SUPERVISOR")));

        assertEquals(org.springframework.http.HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    void interventionIsStoredAndWaitingForPartsKeepsTheAssetInRepair() {
        FailureReportRepository reportRepository = mock(FailureReportRepository.class);
        AssetRepository assetRepository = mock(AssetRepository.class);
        UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
        WorkGroupRepository workGroupRepository = mock(WorkGroupRepository.class);
        MaintenanceHistoryRepository historyRepository = mock(MaintenanceHistoryRepository.class);
        UserRoleService userRoleService = mock(UserRoleService.class);
        FailureEvidenceStorageService evidenceStorageService = mock(FailureEvidenceStorageService.class);
        Usuario technician = user(9L, "tecnico@example.com", "TECNICO");
        Asset asset = new Asset();
        asset.setId(7L);
        asset.setName("Tractor");
        FailureReport report = new FailureReport();
        report.setId(44L);
        report.setAsset(asset);
        report.setUser(technician);
        report.setAssignedTechnician(technician);
        report.setFechaReporte(LocalDateTime.parse("2026-10-05T08:00:00"));
        report.setEstado(FailureStatus.EN_REVISION);
        report.setFechaInicioRevision(LocalDateTime.parse("2026-10-05T08:30:00"));
        when(reportRepository.findById(44L)).thenReturn(Optional.of(report));
        when(reportRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(usuarioRepository.findByEmail(technician.getEmail())).thenReturn(technician);
        when(userRoleService.getEffectiveRoles(technician)).thenReturn(Set.of("TECNICO"));
        when(historyRepository.findByFailureReportId(44L)).thenReturn(Optional.empty());
        when(historyRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        FailureReportService service = new FailureReportService(
                reportRepository, assetRepository, usuarioRepository, workGroupRepository, historyRepository,
                evidenceStorageService, userRoleService, mock(AssetService.class));
        FailureInterventionRequest request = new FailureInterventionRequest(
                "Tractor John Deere, modelo 3036E",
                "No arranca",
                report.getFechaReporte(),
                report.getFechaInicioRevision(),
                "Batería agotada",
                "Desgaste natural",
                "Se desmontó y cambió la batería",
                List.of(new MaintenancePartRequest(
                        "BAT-10", "Batería", "Marca A", BigDecimal.ONE)),
                45,
                "Arranque y prueba en vacío",
                EquipmentFinalState.INOPERATIVE_WAITING_PART,
                "Cambiar alternador cuando llegue el repuesto",
                "Revisar alternador",
                technician.getNombre(),
                "Supervisor de zona");

        var response = service.saveIntervention(
                44L, request, authentication(technician.getEmail(), "ROLE_TECNICO"));

        assertEquals(FailureStatus.ESPERA_REPUESTO, response.estado());
        assertEquals(OperationalStatus.IN_REPAIR, asset.getOperationalStatus());
        verify(historyRepository).save(any(MaintenanceHistory.class));
    }

    private FailureReportRequest request(FailurePriority priority) {
        return new FailureReportRequest(7L, "Falla mecánica", priority, "Descripción de prueba", 5L, null);
    }

    private Usuario user(Long id, String email, String role) {
        Usuario user = new Usuario();
        user.setId(id);
        user.setEmail(email);
        user.setNombre(email);
        user.setRol(role);
        user.setActivo(true);
        return user;
    }

    private Authentication authentication(String email, String role) {
        return new UsernamePasswordAuthenticationToken(email, null, Set.of(new SimpleGrantedAuthority(role)));
    }

    @Test
    void interventionWithInventoryPartDeductsStockAndCreatesMovement() {
        FailureReportRepository reportRepository = mock(FailureReportRepository.class);
        AssetRepository assetRepository = mock(AssetRepository.class);
        UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
        WorkGroupRepository workGroupRepository = mock(WorkGroupRepository.class);
        MaintenanceHistoryRepository historyRepository = mock(MaintenanceHistoryRepository.class);
        UserRoleService userRoleService = mock(UserRoleService.class);
        FailureEvidenceStorageService evidenceStorageService = mock(FailureEvidenceStorageService.class);
        com.agrosentinel.agrosentinel.repositories.InventoryItemRepository inventoryItemRepository = mock(com.agrosentinel.agrosentinel.repositories.InventoryItemRepository.class);
        com.agrosentinel.agrosentinel.repositories.InventoryMovementRepository inventoryMovementRepository = mock(com.agrosentinel.agrosentinel.repositories.InventoryMovementRepository.class);

        Usuario technician = user(9L, "tecnico@example.com", "TECNICO");
        Asset asset = new Asset();
        asset.setId(7L);
        asset.setName("Tractor");
        FailureReport report = new FailureReport();
        report.setId(45L);
        report.setAsset(asset);
        report.setUser(technician);
        report.setAssignedTechnician(technician);
        report.setFechaReporte(LocalDateTime.parse("2026-10-05T08:00:00"));
        report.setEstado(FailureStatus.EN_REVISION);
        report.setFechaInicioRevision(LocalDateTime.parse("2026-10-05T08:30:00"));

        com.agrosentinel.agrosentinel.models.InventoryItem item = new com.agrosentinel.agrosentinel.models.InventoryItem();
        item.setId(101L);
        item.setName("Filtro de Aceite");
        item.setStock(new BigDecimal("10.000"));
        item.setUnit("und");
        WorkGroup inventoryGroup = new WorkGroup();
        inventoryGroup.setId(1L);
        inventoryGroup.setName("Zona Norte");
        item.setWorkGroup(inventoryGroup);

        when(reportRepository.findById(45L)).thenReturn(Optional.of(report));
        when(reportRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(usuarioRepository.findByEmail(technician.getEmail())).thenReturn(technician);
        when(userRoleService.getEffectiveRoles(technician)).thenReturn(Set.of("TECNICO"));
        when(historyRepository.findByFailureReportId(45L)).thenReturn(Optional.empty());
        when(historyRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(inventoryItemRepository.findByIdForUpdate(101L)).thenReturn(Optional.of(item));

        FailureReportService service = new FailureReportService(
                reportRepository, assetRepository, usuarioRepository, workGroupRepository, historyRepository,
                evidenceStorageService, userRoleService, mock(AssetService.class),
                inventoryItemRepository, inventoryMovementRepository);

        FailureInterventionRequest request = new FailureInterventionRequest(
                "Tractor John Deere",
                "Fuga de aceite",
                report.getFechaReporte(),
                report.getFechaInicioRevision(),
                "Filtro desgastado",
                "Uso prolongado",
                "Reemplazo de filtro",
                List.of(new MaintenancePartRequest(
                        "FLT-100", "Filtro de Aceite", "Bosch", new BigDecimal("2.000"), 101L, 1L, "Zona Norte")),
                30,
                "Prueba de encendido y presi�n",
                EquipmentFinalState.OPERATIONAL,
                "Revisar nivel en 50 horas",
                null,
                technician.getNombre(),
                "Supervisor");

        var response = service.saveIntervention(
                45L, request, authentication(technician.getEmail(), "ROLE_TECNICO"));

        assertEquals(FailureStatus.RESUELTO, response.estado());
        assertEquals(new BigDecimal("8.000"), item.getStock());
        verify(inventoryItemRepository).save(item);
        verify(inventoryMovementRepository).save(any(com.agrosentinel.agrosentinel.models.InventoryMovement.class));
    }

    @Test
    void interventionWithInsufficientStockThrowsBadRequest() {
        FailureReportRepository reportRepository = mock(FailureReportRepository.class);
        AssetRepository assetRepository = mock(AssetRepository.class);
        UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
        WorkGroupRepository workGroupRepository = mock(WorkGroupRepository.class);
        MaintenanceHistoryRepository historyRepository = mock(MaintenanceHistoryRepository.class);
        UserRoleService userRoleService = mock(UserRoleService.class);
        FailureEvidenceStorageService evidenceStorageService = mock(FailureEvidenceStorageService.class);
        com.agrosentinel.agrosentinel.repositories.InventoryItemRepository inventoryItemRepository = mock(com.agrosentinel.agrosentinel.repositories.InventoryItemRepository.class);
        com.agrosentinel.agrosentinel.repositories.InventoryMovementRepository inventoryMovementRepository = mock(com.agrosentinel.agrosentinel.repositories.InventoryMovementRepository.class);

        Usuario technician = user(9L, "tecnico@example.com", "TECNICO");
        Asset asset = new Asset();
        asset.setId(7L);
        asset.setName("Tractor");
        FailureReport report = new FailureReport();
        report.setId(46L);
        report.setAsset(asset);
        report.setUser(technician);
        report.setAssignedTechnician(technician);
        report.setFechaReporte(LocalDateTime.parse("2026-10-05T08:00:00"));
        report.setEstado(FailureStatus.EN_REVISION);
        report.setFechaInicioRevision(LocalDateTime.parse("2026-10-05T08:30:00"));

        com.agrosentinel.agrosentinel.models.InventoryItem item = new com.agrosentinel.agrosentinel.models.InventoryItem();
        item.setId(102L);
        item.setName("Filtro de Aire");
        item.setStock(new BigDecimal("1.000"));
        item.setUnit("und");

        when(reportRepository.findById(46L)).thenReturn(Optional.of(report));
        when(usuarioRepository.findByEmail(technician.getEmail())).thenReturn(technician);
        when(userRoleService.getEffectiveRoles(technician)).thenReturn(Set.of("TECNICO"));
        when(inventoryItemRepository.findByIdForUpdate(102L)).thenReturn(Optional.of(item));

        FailureReportService service = new FailureReportService(
                reportRepository, assetRepository, usuarioRepository, workGroupRepository, historyRepository,
                evidenceStorageService, userRoleService, mock(AssetService.class),
                inventoryItemRepository, inventoryMovementRepository);

        FailureInterventionRequest request = new FailureInterventionRequest(
                "Tractor John Deere",
                "Filtro obstruido",
                report.getFechaReporte(),
                report.getFechaInicioRevision(),
                "Filtro saturado",
                "Polvo excesivo",
                "Cambio de filtro",
                List.of(new MaintenancePartRequest(
                        "AIR-10", "Filtro de Aire", "Donaldson", new BigDecimal("5.000"), 102L, null, "Bodega Central")),
                20,
                "Prueba de flujo",
                EquipmentFinalState.OPERATIONAL,
                "Monitorear",
                null,
                technician.getNombre(),
                "Supervisor");

        org.junit.jupiter.api.Assertions.assertThrows(
                org.springframework.web.server.ResponseStatusException.class,
                () -> service.saveIntervention(46L, request, authentication(technician.getEmail(), "ROLE_TECNICO")));
    }
}