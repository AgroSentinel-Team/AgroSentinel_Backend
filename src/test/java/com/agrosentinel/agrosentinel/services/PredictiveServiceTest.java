package com.agrosentinel.agrosentinel.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import tools.jackson.databind.ObjectMapper;

import com.agrosentinel.agrosentinel.dtos.PredictiveRecordRequest;
import com.agrosentinel.agrosentinel.dtos.PredictiveRecordResponse;
import com.agrosentinel.agrosentinel.models.Asset;
import com.agrosentinel.agrosentinel.models.PredictiveRecord;
import com.agrosentinel.agrosentinel.repositories.AssetRepository;
import com.agrosentinel.agrosentinel.repositories.PredictiveRecordRepository;

class PredictiveServiceTest {
    @Test
    void createAcceptsZeroPaddedMachineIdAndStoresCanonicalId() throws Exception {
        AssetRepository assetRepository = mock(AssetRepository.class);
        PredictiveRecordRepository recordRepository = mock(PredictiveRecordRepository.class);
        Asset asset = new Asset();
        asset.setId(2L);
        asset.setHasMotor(true);

        when(assetRepository.findById(2L)).thenReturn(Optional.of(asset));
        when(recordRepository.save(any(PredictiveRecord.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PredictiveService service = new PredictiveService(assetRepository, recordRepository);
        ReflectionTestUtils.setField(service, "apiToken", "predictive-token");
        PredictiveRecordRequest request = new PredictiveRecordRequest(
                2L, "MOTOR-02", LocalDateTime.of(2026, 10, 3, 19, 30),
                115.5, 45.0, 40.0, 2.5, 3.2, 1, 1, "Overheating",
                "Alert", 0.99, "Overheating", "Overheating", 0.87,
                "Dos de tres modelos coinciden en Overheating.");

        PredictiveRecordResponse response = service.create(request, "predictive-token");

        assertEquals("MOTOR-2", response.machineId());
        assertEquals("Overheating", response.tendenciaLstm());
        assertEquals(0.87, response.confianzaLstm());
        assertEquals("Dos de tres modelos coinciden en Overheating.", response.razonConfianza());
        String responseJson = new ObjectMapper().writeValueAsString(response);
        assertTrue(responseJson.contains("\"tendenciaLstm\":\"Overheating\""));
        assertTrue(responseJson.contains("\"confianzaLstm\":0.87"));
        assertTrue(responseJson.contains("\"razonConfianza\":\"Dos de tres modelos coinciden en Overheating.\""));
    }

    @Test
    void requestMapsSnakeCaseLstmFields() throws Exception {
        PredictiveRecordRequest request = new ObjectMapper().readValue(
                """
                {
                  "tendencia_lstm": "Vibration Issue",
                  "confianza_lstm": 0.76,
                  "razon_confianza": "Dos de tres modelos coinciden."
                }
                """,
                PredictiveRecordRequest.class);

        assertEquals("Vibration Issue", request.tendenciaLstm());
        assertEquals(0.76, request.confianzaLstm());
        assertEquals("Dos de tres modelos coinciden.", request.razonConfianza());
    }
}
