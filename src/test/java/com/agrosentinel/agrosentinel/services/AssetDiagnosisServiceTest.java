package com.agrosentinel.agrosentinel.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;

import com.agrosentinel.agrosentinel.dtos.AssetDiagnosisRequest;
import com.agrosentinel.agrosentinel.dtos.DiagnosisNeedRequest;
import com.agrosentinel.agrosentinel.dtos.DiagnosisOrderRequest;
import com.agrosentinel.agrosentinel.dtos.FailureReportRequest;
import com.agrosentinel.agrosentinel.dtos.FailureReportResponse;
import com.agrosentinel.agrosentinel.dtos.PreventiveMaintenanceCreateRequest;
import com.agrosentinel.agrosentinel.dtos.PreventiveMaintenanceResponse;
import com.agrosentinel.agrosentinel.models.Asset;
import com.agrosentinel.agrosentinel.models.AssetDiagnosis;
import com.agrosentinel.agrosentinel.models.DiagnosisNeed;
import com.agrosentinel.agrosentinel.models.DiagnosisRecommendation;
import com.agrosentinel.agrosentinel.models.FailurePriority;
import com.agrosentinel.agrosentinel.models.FailureReport;
import com.agrosentinel.agrosentinel.models.FailureStatus;
import com.agrosentinel.agrosentinel.models.InterventionType;
import com.agrosentinel.agrosentinel.models.MaintenanceAttentionIndex;
import com.agrosentinel.agrosentinel.models.NeedPriority;
import com.agrosentinel.agrosentinel.models.NeedStatus;
import com.agrosentinel.agrosentinel.models.OperationalStatus;
import com.agrosentinel.agrosentinel.models.PreventiveMaintenance;
import com.agrosentinel.agrosentinel.models.Usuario;
import com.agrosentinel.agrosentinel.repositories.AssetDiagnosisRepository;
import com.agrosentinel.agrosentinel.repositories.AssetRepository;
import com.agrosentinel.agrosentinel.repositories.DiagnosisNeedRepository;
import com.agrosentinel.agrosentinel.repositories.FailureReportRepository;
import com.agrosentinel.agrosentinel.repositories.PreventiveMaintenanceRepository;
import com.agrosentinel.agrosentinel.repositories.UsuarioRepository;

class AssetDiagnosisServiceTest {
    private final AssetDiagnosisRepository diagnosisRepository = mock(AssetDiagnosisRepository.class);
    private final DiagnosisNeedRepository needRepository = mock(DiagnosisNeedRepository.class);
    private final AssetRepository assetRepository = mock(AssetRepository.class);
    private final UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
    private final FailureReportRepository failureReportRepository = mock(FailureReportRepository.class);
    private final PreventiveMaintenanceRepository maintenanceRepository = mock(PreventiveMaintenanceRepository.class);
    private final FailureReportService failureReportService = mock(FailureReportService.class);
    private final PreventiveMaintenanceService maintenanceService = mock(PreventiveMaintenanceService.class);
    private final Authentication authentication = mock(Authentication.class);
    private final AssetDiagnosisService service = new AssetDiagnosisService(diagnosisRepository, needRepository,
            assetRepository, usuarioRepository, failureReportRepository, maintenanceRepository,
            failureReportService, maintenanceService);

    @Test
    void classificationLabelsMatchTheTechnicalReport() {
        assertEquals("OPERATIVO – BUEN ESTADO",
                AssetDiagnosisService.classificationLabel(OperationalStatus.OPERATIONAL, DiagnosisRecommendation.KEEP));
        assertEquals("OPERATIVO – REQUIERE MANTENIMIENTO", AssetDiagnosisService.classificationLabel(
                OperationalStatus.OPERATIONAL_WITH_RESTRICTIONS, DiagnosisRecommendation.KEEP));
        assertEquals("FUERA DE SERVICIO – RECUPERABLE", AssetDiagnosisService.classificationLabel(
                OperationalStatus.OUT_OF_SERVICE, DiagnosisRecommendation.RECOVER));
        assertEquals("FUERA DE SERVICIO – RECOMENDADO PARA EVALUACIÓN DE BAJA", AssetDiagnosisService.classificationLabel(
                OperationalStatus.OUT_OF_SERVICE, DiagnosisRecommendation.EVALUATE_DISPOSAL));
        assertEquals("FUERA DE SERVICIO – OBSOLETO – RECOMENDADO PARA BAJA", AssetDiagnosisService.classificationLabel(
                OperationalStatus.OBSOLETE, DiagnosisRecommendation.RECOMMENDED_DISPOSAL));
    }

    @Test
    void rejectsIncoherentClassifications() {
        assertThrows(ResponseStatusException.class, () -> AssetDiagnosisService.validateClassification(
                OperationalStatus.OPERATIONAL, DiagnosisRecommendation.EVALUATE_DISPOSAL, null));
        assertThrows(ResponseStatusException.class, () -> AssetDiagnosisService.validateClassification(
                OperationalStatus.OPERATIONAL_WITH_RESTRICTIONS, DiagnosisRecommendation.RECOVER, null));
        ResponseStatusException noSupport = assertThrows(ResponseStatusException.class,
                () -> AssetDiagnosisService.validateClassification(
                        OperationalStatus.OBSOLETE, DiagnosisRecommendation.RECOMMENDED_DISPOSAL, " "));
        assertTrue(noSupport.getReason().contains("soporte"));
        AssetDiagnosisService.validateClassification(
                OperationalStatus.OUT_OF_SERVICE, DiagnosisRecommendation.EVALUATE_DISPOSAL, null);
        AssetDiagnosisService.validateClassification(
                OperationalStatus.IN_REPAIR, DiagnosisRecommendation.RECOVER, null);
    }

    @Test
    void createRecordsInspectorNeedsAndUpdatesTheAssetStatus() {
        Asset asset = asset();
        when(assetRepository.findById(5L)).thenReturn(Optional.of(asset));
        Usuario inspector = new Usuario();
        inspector.setNombre("Coordinador Granja");
        when(authentication.getName()).thenReturn("coordinador@campo.test");
        when(usuarioRepository.findByEmail("coordinador@campo.test")).thenReturn(inspector);
        when(diagnosisRepository.save(any(AssetDiagnosis.class))).thenAnswer(invocation -> {
            AssetDiagnosis saved = invocation.getArgument(0);
            saved.setId(30L);
            return saved;
        });
        when(diagnosisRepository.findAllByAssetIdOrderByDiagnosedOnDescIdDesc(5L)).thenAnswer(invocation -> List.of());

        var response = service.create(request(5L, List.of(
                new DiagnosisNeedRequest(null, " Cambio de llantas delanteras ",
                        List.of(InterventionType.CORRECTIVE, InterventionType.CORRECTIVE), NeedPriority.ALTA, true, null),
                new DiagnosisNeedRequest(null, "Reparación del escape", List.of(InterventionType.CORRECTIVE),
                        NeedPriority.MEDIA, false, null))), authentication);

        assertEquals("Coordinador Granja", response.inspectorName());
        assertEquals("OPERATIVO – REQUIERE MANTENIMIENTO", response.classificationLabel());
        assertEquals(2, response.needs().size());
        assertEquals("Cambio de llantas delanteras", response.needs().get(0).description());
        assertEquals(List.of(InterventionType.CORRECTIVE), response.needs().get(0).interventionTypes());
        assertEquals(OperationalStatus.OPERATIONAL_WITH_RESTRICTIONS, asset.getOperationalStatus());
    }

    @Test
    void olderDiagnosisDoesNotOverrideTheCurrentAssetStatus() {
        Asset asset = asset();
        asset.setOperationalStatus(OperationalStatus.OUT_OF_SERVICE);
        when(assetRepository.findById(5L)).thenReturn(Optional.of(asset));
        when(authentication.getName()).thenReturn("admin@campo.test");
        when(diagnosisRepository.save(any(AssetDiagnosis.class))).thenAnswer(invocation -> {
            AssetDiagnosis saved = invocation.getArgument(0);
            saved.setId(31L);
            return saved;
        });
        AssetDiagnosis newer = new AssetDiagnosis();
        newer.setId(99L);
        when(diagnosisRepository.findAllByAssetIdOrderByDiagnosedOnDescIdDesc(5L)).thenReturn(List.of(newer));

        service.create(request(5L, List.of()), authentication);

        assertEquals(OperationalStatus.OUT_OF_SERVICE, asset.getOperationalStatus());
        verify(assetRepository, never()).save(any());
    }

    @Test
    void updateKeepsExistingNeedsInPlaceAndRemovesMissingOnes() {
        Asset asset = asset();
        AssetDiagnosis diagnosis = new AssetDiagnosis();
        diagnosis.setId(40L);
        diagnosis.setAsset(asset);
        diagnosis.setDiagnosedOn(LocalDate.now());
        DiagnosisNeed tires = need(1L, diagnosis, "Cambio de llantas");
        FailureReport linked = new FailureReport();
        linked.setId(12L);
        linked.setEstado(FailureStatus.ASIGNADA);
        tires.setFailureReport(linked);
        DiagnosisNeed exhaust = need(2L, diagnosis, "Reparación del escape");
        diagnosis.getNeeds().addAll(List.of(tires, exhaust));
        when(diagnosisRepository.findById(40L)).thenReturn(Optional.of(diagnosis));
        when(diagnosisRepository.save(diagnosis)).thenReturn(diagnosis);
        when(diagnosisRepository.findAllByAssetIdOrderByDiagnosedOnDescIdDesc(5L)).thenReturn(List.of(diagnosis));

        var response = service.update(40L, request(5L, List.of(
                new DiagnosisNeedRequest(1L, "Cambio de llantas delanteras", List.of(InterventionType.CORRECTIVE),
                        NeedPriority.ALTA, true, null))));

        assertEquals(1, diagnosis.getNeeds().size());
        assertSame(tires, diagnosis.getNeeds().getFirst());
        assertEquals("F-12", response.needs().getFirst().orderLabel());
        assertEquals("Cambio de llantas delanteras", tires.getDescription());
    }

    @Test
    void correctiveOrderCreatesALinkedFailureReport() {
        DiagnosisNeed need = linkedNeed();
        when(needRepository.findById(7L)).thenReturn(Optional.of(need));
        when(needRepository.save(need)).thenReturn(need);
        FailureReportResponse report = mock(FailureReportResponse.class);
        when(report.id()).thenReturn(88L);
        when(failureReportService.create(any(), isNull(), eq(authentication))).thenReturn(report);
        FailureReport reference = new FailureReport();
        reference.setId(88L);
        reference.setEstado(FailureStatus.ASIGNADA);
        when(failureReportRepository.getReferenceById(88L)).thenReturn(reference);

        var response = service.createOrder(7L,
                new DiagnosisOrderRequest(DiagnosisOrderRequest.OrderType.CORRECTIVE, 3L, null, null), authentication);

        ArgumentCaptor<FailureReportRequest> sent = ArgumentCaptor.forClass(FailureReportRequest.class);
        verify(failureReportService).create(sent.capture(), isNull(), eq(authentication));
        assertEquals(FailurePriority.ALTA, sent.getValue().prioridad());
        assertEquals(3L, sent.getValue().groupId());
        assertEquals("Cambio de llantas delanteras", sent.getValue().tipoFalla());
        assertTrue(sent.getValue().descripcion().contains("seguridad del operador"));
        assertEquals("F-88", response.orderLabel());
        assertEquals(NeedStatus.IN_PROGRESS, response.status());

        assertThrows(ResponseStatusException.class, () -> service.createOrder(7L,
                new DiagnosisOrderRequest(DiagnosisOrderRequest.OrderType.CORRECTIVE, 3L, null, null), authentication));
    }

    @Test
    void preventiveOrderNeedsTechnicianAndDate() {
        DiagnosisNeed need = linkedNeed();
        when(needRepository.findById(7L)).thenReturn(Optional.of(need));
        when(needRepository.save(need)).thenReturn(need);
        assertThrows(ResponseStatusException.class, () -> service.createOrder(7L,
                new DiagnosisOrderRequest(DiagnosisOrderRequest.OrderType.PREVENTIVE, null, 4L, null), authentication));

        PreventiveMaintenanceResponse maintenance = mock(PreventiveMaintenanceResponse.class);
        when(maintenance.id()).thenReturn(21L);
        when(maintenanceService.schedule(any(), eq(authentication))).thenReturn(maintenance);
        PreventiveMaintenance reference = new PreventiveMaintenance();
        reference.setId(21L);
        when(maintenanceRepository.getReferenceById(21L)).thenReturn(reference);

        var response = service.createOrder(7L, new DiagnosisOrderRequest(DiagnosisOrderRequest.OrderType.PREVENTIVE,
                null, 4L, LocalDateTime.of(2026, 12, 1, 8, 0)), authentication);

        ArgumentCaptor<PreventiveMaintenanceCreateRequest> sent =
                ArgumentCaptor.forClass(PreventiveMaintenanceCreateRequest.class);
        verify(maintenanceService).schedule(sent.capture(), eq(authentication));
        assertEquals(MaintenanceAttentionIndex.HIGH, sent.getValue().attentionIndex());
        assertEquals("MP-21", response.orderLabel());
    }

    @Test
    void needIsDoneWhenItsOrderIsResolved() {
        DiagnosisNeed need = linkedNeed();
        need.setStatus(NeedStatus.IN_PROGRESS);
        FailureReport report = new FailureReport();
        report.setId(5L);
        report.setEstado(FailureStatus.RESUELTO);
        need.setFailureReport(report);
        when(needRepository.findById(7L)).thenReturn(Optional.of(need));
        when(needRepository.save(need)).thenReturn(need);

        assertEquals(NeedStatus.DONE, service.updateNeedStatus(7L, NeedStatus.IN_PROGRESS).status());
    }

    private static Asset asset() {
        Asset asset = new Asset();
        asset.setId(5L);
        asset.setName("Tractor Massey Ferguson 290");
        asset.setOperationalStatus(OperationalStatus.OPERATIONAL);
        return asset;
    }

    private static AssetDiagnosisRequest request(Long assetId, List<DiagnosisNeedRequest> needs) {
        return new AssetDiagnosisRequest(assetId, LocalDate.now().minusDays(1), "En funcionamiento.",
                "Reemplazar llantas delanteras.", "Viable para continuar en operación.", "Operativo.",
                OperationalStatus.OPERATIONAL_WITH_RESTRICTIONS, DiagnosisRecommendation.KEEP,
                "Atender con prioridad el cambio de las llantas delanteras.", null, null, needs);
    }

    private static DiagnosisNeed need(Long id, AssetDiagnosis diagnosis, String description) {
        DiagnosisNeed need = new DiagnosisNeed();
        need.setId(id);
        need.setDiagnosis(diagnosis);
        need.setDescription(description);
        need.setInterventionTypes("CORRECTIVE");
        need.setPriority(NeedPriority.MEDIA);
        return need;
    }

    private static DiagnosisNeed linkedNeed() {
        AssetDiagnosis diagnosis = new AssetDiagnosis();
        diagnosis.setAsset(asset());
        diagnosis.setDiagnosedOn(LocalDate.of(2026, 11, 20));
        DiagnosisNeed need = need(7L, diagnosis, "Cambio de llantas delanteras");
        need.setPriority(NeedPriority.ALTA);
        need.setSafetyRelated(true);
        return need;
    }
}
