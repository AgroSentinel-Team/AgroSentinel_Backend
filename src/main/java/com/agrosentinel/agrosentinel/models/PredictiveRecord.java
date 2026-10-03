package com.agrosentinel.agrosentinel.models;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "predictive_records")
public class PredictiveRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asset_id", nullable = false)
    private Asset asset;

    @Column(name = "machine_id", nullable = false)
    private String machineId;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(nullable = false)
    private double temperature;

    @Column(nullable = false)
    private double vibration;

    @Column(nullable = false)
    private double humidity;

    @Column(nullable = false)
    private double pressure;

    @Column(name = "energy_consumption", nullable = false)
    private double energyConsumption;

    @Column(name = "machine_status", nullable = false)
    private int machineStatus;

    @Column(name = "prediction_class", nullable = false)
    private int predictionClass;

    @Column(name = "tipo_falla_predicha")
    private String tipoFallaPredicha;

    @Column(name = "falla_real_confirmada")
    private String fallaRealConfirmada;

    @Column(name = "motor_state", nullable = false)
    private String motorState;

    @Column(nullable = false, columnDefinition = "double precision")
    private double confidence;

    @Column(name = "affected_components", nullable = false, columnDefinition = "text")
    private String affectedComponents;

    @PrePersist
    private void applyTimestamp() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
    }
}