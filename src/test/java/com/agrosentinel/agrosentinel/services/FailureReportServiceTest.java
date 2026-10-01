package com.agrosentinel.agrosentinel.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.agrosentinel.agrosentinel.dtos.FailureReportRequest;
import com.agrosentinel.agrosentinel.models.Asset;
import com.agrosentinel.agrosentinel.models.FailurePriority;
import com.agrosentinel.agrosentinel.models.OperationalStatus;
import com.agrosentinel.agrosentinel.models.Usuario;
import com.agrosentinel.agrosentinel.repositories.AssetRepository;
import com.agrosentinel.agrosentinel.repositories.FailureReportRepository;
import com.agrosentinel.agrosentinel.repositories.UsuarioRepository;

class FailureReportServiceTest {
    @Test
    void onlyHighAndCriticalPriorityMoveAssetToRepair() {
        FailureReportRepository reportRepository = mock(FailureReportRepository.class);
        AssetRepository assetRepository = mock(AssetRepository.class);
        UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
        FailureEvidenceStorageService evidenceStorageService = mock(FailureEvidenceStorageService.class);
        Asset asset = new Asset();
        asset.setId(7L);
        asset.setName("Tractor de prueba");
        Usuario user = new Usuario();
        user.setId(3L);
        user.setEmail("operario@example.com");
        user.setNombre("Operario");

        when(assetRepository.findById(anyLong())).thenReturn(Optional.of(asset));
        when(usuarioRepository.findByEmail(user.getEmail())).thenReturn(user);
        when(reportRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        FailureReportService service = new FailureReportService(
                reportRepository, assetRepository, usuarioRepository, evidenceStorageService);

        service.create(request(FailurePriority.ALTA), null, user.getEmail());
        assertEquals(OperationalStatus.IN_REPAIR, asset.getOperationalStatus());

        asset.setOperationalStatus(OperationalStatus.OPERATIONAL);
        service.create(request(FailurePriority.CRITICA), null, user.getEmail());
        assertEquals(OperationalStatus.IN_REPAIR, asset.getOperationalStatus());

        asset.setOperationalStatus(OperationalStatus.OPERATIONAL);
        service.create(request(FailurePriority.MEDIA), null, user.getEmail());
        assertEquals(OperationalStatus.OPERATIONAL, asset.getOperationalStatus());
    }

    private FailureReportRequest request(FailurePriority priority) {
        return new FailureReportRequest(7L, "Falla mecánica", priority, "Descripción de prueba");
    }
}
