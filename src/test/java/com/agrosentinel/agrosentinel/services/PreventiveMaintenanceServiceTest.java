package com.agrosentinel.agrosentinel.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.server.ResponseStatusException;

import com.agrosentinel.agrosentinel.dtos.PreventiveMaintenanceCreateRequest;
import com.agrosentinel.agrosentinel.dtos.PreventiveMaintenanceExecutionRequest;
import com.agrosentinel.agrosentinel.dtos.PreventiveMaintenanceTaskRequest;
import com.agrosentinel.agrosentinel.models.Asset;
import com.agrosentinel.agrosentinel.models.Category;
import com.agrosentinel.agrosentinel.models.MaintenanceAttentionIndex;
import com.agrosentinel.agrosentinel.models.MaintenanceHistory;
import com.agrosentinel.agrosentinel.models.MaintenanceHistoryStatus;
import com.agrosentinel.agrosentinel.models.MaintenanceOutcome;
import com.agrosentinel.agrosentinel.models.PreventiveMaintenance;
import com.agrosentinel.agrosentinel.models.PreventiveMaintenanceStatus;
import com.agrosentinel.agrosentinel.models.Usuario;
import com.agrosentinel.agrosentinel.repositories.AssetRepository;
import com.agrosentinel.agrosentinel.repositories.MaintenanceHistoryRepository;
import com.agrosentinel.agrosentinel.repositories.PreventiveMaintenanceRepository;
import com.agrosentinel.agrosentinel.repositories.UsuarioRepository;

class PreventiveMaintenanceServiceTest {
    @Test
    void supervisorCanReadAllPreventiveMaintenances() {
        PreventiveMaintenanceRepository maintenanceRepository = mock(PreventiveMaintenanceRepository.class);
        when(maintenanceRepository.findAllByOrderByScheduledAtAsc()).thenReturn(List.of());
        PreventiveMaintenanceService service = service(
                maintenanceRepository,
                mock(AssetRepository.class),
                mock(UsuarioRepository.class),
                mock(MaintenanceHistoryRepository.class),
                mock(PreventiveMaintenanceEvidenceStorageService.class));

        assertEquals(List.of(), service.findAll(authentication("supervisor@example.com", "ROLE_SUPERVISOR")));
        org.mockito.Mockito.verify(maintenanceRepository).findAllByOrderByScheduledAtAsc();
    }

    @Test
    void supervisorCannotSchedulePreventiveMaintenance() {
        PreventiveMaintenanceRepository maintenanceRepository = mock(PreventiveMaintenanceRepository.class);
        PreventiveMaintenanceService service = service(
                maintenanceRepository,
                mock(AssetRepository.class),
                mock(UsuarioRepository.class),
                mock(MaintenanceHistoryRepository.class),
                mock(PreventiveMaintenanceEvidenceStorageService.class));

        assertThrows(ResponseStatusException.class, () -> service.schedule(
                new PreventiveMaintenanceCreateRequest(
                        7L,
                        3L,
                        LocalDateTime.of(2026, 11, 1, 9, 30),
                        MaintenanceAttentionIndex.LOW,
                        null),
                authentication("supervisor@example.com", "ROLE_SUPERVISOR")));
        org.mockito.Mockito.verify(maintenanceRepository, org.mockito.Mockito.never())
                .save(any(PreventiveMaintenance.class));
    }

    @Test
    void scheduleCreatesLinkedHistoryEntryWithScheduledStatus() {
        PreventiveMaintenanceRepository maintenanceRepository = mock(PreventiveMaintenanceRepository.class);
        AssetRepository assetRepository = mock(AssetRepository.class);
        UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
        MaintenanceHistoryRepository historyRepository = mock(MaintenanceHistoryRepository.class);
        PreventiveMaintenanceEvidenceStorageService evidenceStorageService =
                mock(PreventiveMaintenanceEvidenceStorageService.class);

        Asset asset = asset();
        Usuario technician = technician();
        LocalDateTime scheduledAt = LocalDateTime.of(2026, 11, 1, 9, 30);
        when(assetRepository.findById(7L)).thenReturn(Optional.of(asset));
        when(usuarioRepository.findById(3L)).thenReturn(Optional.of(technician));
        when(maintenanceRepository.save(any(PreventiveMaintenance.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        Authentication administrator = authentication("admin@example.com", "ROLE_ADMIN");

        PreventiveMaintenanceService service = service(
                maintenanceRepository, assetRepository, usuarioRepository, historyRepository, evidenceStorageService);
        service.schedule(
                new PreventiveMaintenanceCreateRequest(
                        7L, 3L, scheduledAt, MaintenanceAttentionIndex.HIGH, "Riesgo predictivo: Alto"),
                administrator);

        var historyCaptor = org.mockito.ArgumentCaptor.forClass(MaintenanceHistory.class);
        verify(historyRepository).save(historyCaptor.capture());
        MaintenanceHistory history = historyCaptor.getValue();
        assertEquals(MaintenanceHistoryStatus.SCHEDULED, history.getStatus());
        assertEquals(scheduledAt, history.getScheduledAt());
        assertEquals("Técnica de prueba", history.getTechnicianName());
        assertEquals("Tractor de prueba", history.getAsset().getName());
        assertEquals(PreventiveMaintenanceStatus.SCHEDULED, history.getPreventiveMaintenance().getStatus());
        assertEquals(MaintenanceAttentionIndex.HIGH, history.getPreventiveMaintenance().getAttentionIndex());
        assertEquals("Riesgo predictivo: Alto", history.getPreventiveMaintenance().getPredictiveContext());
        org.junit.jupiter.api.Assertions.assertTrue(history.getTasksDescription().contains("Índice de atención: Alta"));
        org.junit.jupiter.api.Assertions.assertTrue(history.getTasksDescription().contains("Riesgo predictivo: Alto"));
    }

    @Test
    void executionUpdatesTheExistingHistoryEntryInsteadOfCreatingAnotherOne() {
        PreventiveMaintenanceRepository maintenanceRepository = mock(PreventiveMaintenanceRepository.class);
        AssetRepository assetRepository = mock(AssetRepository.class);
        UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
        MaintenanceHistoryRepository historyRepository = mock(MaintenanceHistoryRepository.class);
        PreventiveMaintenanceEvidenceStorageService evidenceStorageService =
                mock(PreventiveMaintenanceEvidenceStorageService.class);

        Usuario technician = technician();
        PreventiveMaintenance maintenance = new PreventiveMaintenance();
        maintenance.setId(18L);
        maintenance.setAsset(asset());
        maintenance.setTechnician(technician);
        maintenance.setScheduledAt(LocalDateTime.of(2026, 10, 1, 9, 30));
        MaintenanceHistory history = new MaintenanceHistory();
        history.setAsset(maintenance.getAsset());
        history.setPreventiveMaintenance(maintenance);

        when(maintenanceRepository.findById(18L)).thenReturn(Optional.of(maintenance));
        when(maintenanceRepository.save(any(PreventiveMaintenance.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(historyRepository.findByPreventiveMaintenanceId(18L)).thenReturn(Optional.of(history));
        when(historyRepository.save(any(MaintenanceHistory.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(evidenceStorageService.storeAll(anyList())).thenReturn(List.of());

        PreventiveMaintenanceService service = service(
                maintenanceRepository, assetRepository, usuarioRepository, historyRepository, evidenceStorageService);
        service.execute(
                18L,
                new PreventiveMaintenanceExecutionRequest(
                        List.of(new PreventiveMaintenanceTaskRequest("Revisar presión", true)),
                        "Filtro de aceite",
                        "Se realizó la inspección programada.",
                        "Sin anomalías.",
                        MaintenanceOutcome.COMPLETED,
                        null,
                        true),
                List.of(),
                authentication("tecnico@example.com", "ROLE_TECNICO"));

        assertEquals(MaintenanceHistoryStatus.COMPLETED, history.getStatus());
        assertEquals(maintenance.getExecutedAt(), history.getExecutedAt());
        assertEquals(maintenance.getExecutedAt().toLocalDate(), history.getMaintenanceDate());
        assertEquals("Técnica de prueba", history.getTechnicianName());
        assertEquals("Filtro de aceite", history.getReplacedParts());
        assertEquals(maintenance.getConfirmedAt(), history.getSignedAt());
        assertEquals(maintenance.getExecutedAt().toLocalDate().plusDays(45), history.getNextRecommendedDate());
        verify(historyRepository).save(history);
    }

    private PreventiveMaintenanceService service(
            PreventiveMaintenanceRepository maintenanceRepository,
            AssetRepository assetRepository,
            UsuarioRepository usuarioRepository,
            MaintenanceHistoryRepository historyRepository,
            PreventiveMaintenanceEvidenceStorageService evidenceStorageService) {
        UserRoleService userRoleService = mock(UserRoleService.class);
        when(userRoleService.getEffectiveRoles(any(Usuario.class))).thenReturn(Set.of("TECNICO"));
        return new PreventiveMaintenanceService(maintenanceRepository, assetRepository, usuarioRepository,
                historyRepository, evidenceStorageService, userRoleService);
    }

    private Asset asset() {
        Category category = new Category();
        category.setName("Tractores");
        category.setMaintenanceFrequencyDays(45);
        Asset asset = new Asset();
        asset.setId(7L);
        asset.setName("Tractor de prueba");
        asset.setCategory(category);
        asset.setActive(true);
        return asset;
    }

    private Usuario technician() {
        Usuario technician = new Usuario();
        technician.setId(3L);
        technician.setEmail("tecnico@example.com");
        technician.setNombre("Técnica de prueba");
        technician.setRol("TECNICO");
        technician.setActivo(true);
        return technician;
    }

    private Authentication authentication(String email, String authority) {
        return new UsernamePasswordAuthenticationToken(
                email, null, List.of(new SimpleGrantedAuthority(authority)));
    }
}
