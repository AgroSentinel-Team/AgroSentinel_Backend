package com.agrosentinel.agrosentinel.services;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.agrosentinel.agrosentinel.dtos.PredictiveAssetResponse;
import com.agrosentinel.agrosentinel.dtos.PredictiveFeedbackRequest;
import com.agrosentinel.agrosentinel.dtos.PredictiveRecordRequest;
import com.agrosentinel.agrosentinel.dtos.PredictiveRecordResponse;
import com.agrosentinel.agrosentinel.models.Asset;
import com.agrosentinel.agrosentinel.models.PredictiveRecord;
import com.agrosentinel.agrosentinel.repositories.AssetRepository;
import com.agrosentinel.agrosentinel.repositories.PredictiveRecordRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PredictiveService {
    private final AssetRepository assetRepository;
    private final PredictiveRecordRepository predictiveRecordRepository;

    @Value("${agrosentinel.predictive.api-token}")
    private String apiToken;

    @Value("${agrosentinel.predictive.api-url}")
    private String predictiveApiUrl;

    @Transactional
    public PredictiveRecordResponse create(PredictiveRecordRequest request, String token) {
        if (apiToken.isBlank() || !apiToken.equals(token)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid predictive service token");
        }

        Asset asset = findAsset(request.assetId());
        if (!asset.isHasMotor()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El activo no está marcado como motor");
        }
        String expectedMachineId = machineId(asset);
        if (!expectedMachineId.equals(normalizeMachineId(request.machineId()))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El ID predictivo no corresponde al activo");
        }

        PredictiveRecord record = new PredictiveRecord();
        record.setAsset(asset);
        record.setMachineId(expectedMachineId);
        record.setTimestamp(request.timestamp() == null ? LocalDateTime.now() : request.timestamp());
        record.setTemperature(request.temperature());
        record.setVibration(request.vibration());
        record.setHumidity(request.humidity());
        record.setPressure(request.pressure());
        record.setEnergyConsumption(request.energyConsumption());
        record.setMachineStatus(request.machineStatus());
        record.setPredictionClass(request.predictionClass());
        record.setTipoFallaPredicha(request.tipoFallaPredicha());
        record.setMotorState(request.motorState());
        record.setConfidence(request.confidence());
        record.setAffectedComponents(request.affectedComponents());
        return toResponse(predictiveRecordRepository.save(record));
    }

    public List<PredictiveRecordResponse> history(Long assetId) {
        Asset asset = findAsset(assetId);
        requireMotor(asset);
        return predictiveRecordRepository.findAllByAssetIdOrderByTimestampDesc(assetId)
                .stream().map(this::toResponse).toList();
    }

    public List<PredictiveAssetResponse> motorAssets() {
        return assetRepository.findActiveMotorAssets().stream()
                .map(asset -> new PredictiveAssetResponse(
                        asset.getId(), machineId(asset), asset.getName(), asset.getBrand(), asset.getModel(),
                        asset.getLocation(), predictiveRecordRepository
                                .findFirstByAssetIdOrderByTimestampDesc(asset.getId())
                                .map(this::toResponse).orElse(null)))
                .toList();
    }

    @Transactional
    public PredictiveRecordResponse confirmFeedback(Long id, PredictiveFeedbackRequest request) {
        PredictiveRecord record = predictiveRecordRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "No se encontró la lectura predictiva"));

        try {
            RestClient.create(predictiveApiUrl)
                    .put()
                    .uri("/historial/{id}/feedback", record.getId())
                    .header("X-Predictive-Token", apiToken)
                    .body(new PredictiveFeedbackRequest(
                            request.fallaRealConfirmada(), record.getMachineId(), record.getTimestamp()))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException error) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "No fue posible sincronizar la validación con el servicio predictivo.",
                    error);
        }

        record.setFallaRealConfirmada(request.fallaRealConfirmada().trim());
        return toResponse(predictiveRecordRepository.save(record));
    }

    private Asset findAsset(Long id) {
        return assetRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Activo no encontrado"));
    }

    private void requireMotor(Asset asset) {
        if (!asset.isHasMotor()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El activo no está marcado como motor");
        }
    }

    private String machineId(Asset asset) {
        return "MOTOR-" + asset.getId();
    }

    private String normalizeMachineId(String machineId) {
        return machineId == null ? "" : machineId.replaceFirst("^MOTOR-0+(?=\\d+$)", "MOTOR-");
    }

    private PredictiveRecordResponse toResponse(PredictiveRecord record) {
        return new PredictiveRecordResponse(
                record.getId(), record.getAsset().getId(), record.getMachineId(), record.getTimestamp(),
                record.getTemperature(), record.getVibration(), record.getHumidity(), record.getPressure(),
                record.getEnergyConsumption(), record.getMachineStatus(), record.getPredictionClass(),
                record.getTipoFallaPredicha(), record.getFallaRealConfirmada(), record.getMotorState(),
                record.getConfidence(), record.getAffectedComponents());
    }
}