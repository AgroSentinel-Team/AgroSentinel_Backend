package com.agrosentinel.agrosentinel.models;

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
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Necesidad identificada en un diagnóstico (cambio de llantas, revisión de cardán...). */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "diagnosis_needs")
public class DiagnosisNeed {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "diagnosis_id", nullable = false)
    private AssetDiagnosis diagnosis;

    @Column(nullable = false, length = 255)
    private String description;

    /** Tipos de intervención separados por coma (p. ej. "CORRECTIVE,PREVENTIVE"). */
    @Column(name = "intervention_types", nullable = false, length = 255)
    private String interventionTypes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private NeedPriority priority;

    @Column(name = "safety_related", nullable = false)
    private boolean safetyRelated;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NeedStatus status = NeedStatus.PENDING;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "failure_report_id")
    private FailureReport failureReport;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "preventive_maintenance_id")
    private PreventiveMaintenance preventiveMaintenance;
}
