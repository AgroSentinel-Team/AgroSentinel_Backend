package com.agrosentinel.agrosentinel.models;

import java.math.BigDecimal;
import java.time.LocalDate;
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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "maintenance_history")
public class MaintenanceHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asset_id", nullable = false)
    private Asset asset;

    @Column(name = "maintenance_date", nullable = false)
    private LocalDate maintenanceDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MaintenanceType type;

    @Column(name = "tasks_description", columnDefinition = "text")
    private String tasksDescription;

    private BigDecimal cost;

    private String provider;

    @Column(name = "next_recommended_date")
    private LocalDate nextRecommendedDate;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "preventive_maintenance_id", unique = true)
    private PreventiveMaintenance preventiveMaintenance;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40, columnDefinition = "varchar(40) not null default 'COMPLETED'")
    private MaintenanceHistoryStatus status = MaintenanceHistoryStatus.COMPLETED;

    @Column(name = "scheduled_at")
    private LocalDateTime scheduledAt;

    @Column(name = "executed_at")
    private LocalDateTime executedAt;

    @Column(name = "technician_name", length = 160)
    private String technicianName;

    @Column(name = "replaced_parts", columnDefinition = "text")
    private String replacedParts;

    @Column(name = "signed_at")
    private LocalDateTime signedAt;
}