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
        return new FailureReportRequest(7L, "Falla mecánica", priority, "Descripción de prueba", 5L);
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
}
