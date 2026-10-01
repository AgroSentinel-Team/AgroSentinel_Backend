package com.agrosentinel.agrosentinel.models;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "failure_reports")
public class FailureReport {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asset_id", nullable = false)
    private Asset asset;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private Usuario user;

    @Column(name = "tipo_falla", nullable = false, length = 120)
    private String tipoFalla;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private FailurePriority prioridad;

    @Column(nullable = false, columnDefinition = "text")
    private String descripcion;

    @Column(name = "fecha_reporte", nullable = false)
    private LocalDateTime fechaReporte;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private FailureStatus estado;

    @Column(name = "evidencia_url", length = 512)
    private String evidenciaUrl;

    @PrePersist
    private void applyDefaults() {
        if (fechaReporte == null) fechaReporte = LocalDateTime.now();
        if (estado == null) estado = FailureStatus.PENDIENTE;
    }
}
