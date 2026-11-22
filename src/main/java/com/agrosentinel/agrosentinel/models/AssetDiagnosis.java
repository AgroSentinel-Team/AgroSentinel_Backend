package com.agrosentinel.agrosentinel.models;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.BatchSize;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Ficha técnica de diagnóstico del estado de un activo en una fecha. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "asset_diagnoses")
public class AssetDiagnosis {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asset_id", nullable = false)
    private Asset asset;

    @Column(name = "diagnosed_on", nullable = false)
    private LocalDate diagnosedOn;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inspector_id")
    private Usuario inspector;

    /** Nombre de quien elaboró la ficha, guardado para la firma aunque el usuario cambie. */
    @Column(name = "inspector_name")
    private String inspectorName;

    @Column(name = "current_state_description", columnDefinition = "text")
    private String currentStateDescription;

    @Column(name = "technical_diagnosis", columnDefinition = "text")
    private String technicalDiagnosis;

    @Column(name = "technical_concept", columnDefinition = "text")
    private String technicalConcept;

    @Column(columnDefinition = "text")
    private String conclusion;

    @Enumerated(EnumType.STRING)
    @Column(name = "operational_condition", nullable = false, length = 40)
    private OperationalStatus operationalCondition;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private DiagnosisRecommendation recommendation;

    /** Acciones concretas recomendadas (p. ej. "Atender con prioridad el cambio de llantas"). */
    @Column(name = "recommendation_detail", columnDefinition = "text")
    private String recommendationDetail;

    /** Soporte de la evaluación técnico-económica cuando se recomienda la baja. */
    @Column(name = "disposal_support", columnDefinition = "text")
    private String disposalSupport;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "diagnosis", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC, id ASC")
    @BatchSize(size = 64)
    private List<DiagnosisNeed> needs = new ArrayList<>();

    @PrePersist
    private void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    private void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
