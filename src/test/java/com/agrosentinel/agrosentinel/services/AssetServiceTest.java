package com.agrosentinel.agrosentinel.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.agrosentinel.agrosentinel.models.Asset;
import com.agrosentinel.agrosentinel.models.Category;
import com.agrosentinel.agrosentinel.models.MaintenanceHistory;
import com.agrosentinel.agrosentinel.models.PreventiveMaintenance;
import com.agrosentinel.agrosentinel.models.PreventiveMaintenanceStatus;
import com.agrosentinel.agrosentinel.models.OperationalStatus;
import com.agrosentinel.agrosentinel.models.Usuario;
import com.agrosentinel.agrosentinel.repositories.AssetRepository;
import com.agrosentinel.agrosentinel.repositories.CategoryRepository;
import com.agrosentinel.agrosentinel.repositories.MaintenanceHistoryRepository;
import com.agrosentinel.agrosentinel.repositories.PreventiveMaintenanceRepository;
import com.agrosentinel.agrosentinel.repositories.UsuarioRepository;

class AssetServiceTest {
    @Test
    void resumeBackfillsOldScheduledMaintenanceOnlyOnce() {
        AssetRepository assetRepository = mock(AssetRepository.class);
        CategoryRepository categoryRepository = mock(CategoryRepository.class);
        UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
        MaintenanceHistoryRepository historyRepository = mock(MaintenanceHistoryRepository.class);
        PreventiveMaintenanceRepository maintenanceRepository = mock(PreventiveMaintenanceRepository.class);

        Usuario technician = new Usuario();
        technician.setId(3L);
        technician.setNombre("Técnica asignada");
        Category category = new Category();
        category.setId(2L);
        category.setName("Tractores");
        Asset asset = new Asset();
        asset.setId(7L);
        asset.setName("Tractor de prueba");
        asset.setCategory(category);
        asset.setResponsible(technician);
        asset.setOperationalStatus(OperationalStatus.OPERATIONAL);
        PreventiveMaintenance maintenance = new PreventiveMaintenance();
        maintenance.setId(15L);
        maintenance.setAsset(asset);
        maintenance.setTechnician(technician);
        maintenance.setScheduledAt(LocalDateTime.of(2026, 11, 10, 8, 0));
        maintenance.setStatus(PreventiveMaintenanceStatus.SCHEDULED);

        List<MaintenanceHistory> historyRows = new ArrayList<>();
        when(assetRepository.findById(7L)).thenReturn(Optional.of(asset));
        when(maintenanceRepository.findAllByAssetIdAndStatusOrderByScheduledAtAsc(
                7L, PreventiveMaintenanceStatus.SCHEDULED)).thenReturn(List.of(maintenance));
        when(historyRepository.findByPreventiveMaintenanceId(15L)).thenAnswer(invocation -> historyRows.stream()
                .filter(row -> row.getPreventiveMaintenance().getId().equals(15L))
                .findFirst());
        when(historyRepository.save(any(MaintenanceHistory.class))).thenAnswer(invocation -> {
            MaintenanceHistory history = invocation.getArgument(0);
            historyRows.add(history);
            return history;
        });
        when(historyRepository.findAllByAssetIdOrderByMaintenanceDateDesc(7L))
                .thenAnswer(invocation -> List.copyOf(historyRows));

        AssetService service = new AssetService(
                assetRepository, categoryRepository, usuarioRepository, historyRepository, maintenanceRepository);

        var firstResume = service.getResume(7L);
        var secondResume = service.getResume(7L);

        assertEquals(1, firstResume.maintenanceHistory().size());
        assertEquals(1, secondResume.maintenanceHistory().size());
        assertEquals("SCHEDULED", firstResume.maintenanceHistory().getFirst().status().name());
        assertEquals("Técnica asignada", firstResume.maintenanceHistory().getFirst().technicianName());
        assertEquals(maintenance.getScheduledAt(), firstResume.maintenanceHistory().getFirst().scheduledAt());
        verify(historyRepository, times(1)).save(any(MaintenanceHistory.class));
    }
}
