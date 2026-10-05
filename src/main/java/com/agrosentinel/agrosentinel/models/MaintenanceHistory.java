package com.agrosentinel.agrosentinel.models;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
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

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "failure_report_id", unique = true)
    private FailureReport failureReport;

    @Column(name = "equipment_intervened", length = 1000)
    private String equipmentIntervened;

    @Column(name = "initial_reported_symptom", columnDefinition = "text")
    private String initialReportedSymptom;

    @Column(name = "intervention_reported_at")
    private LocalDateTime interventionReportedAt;

    @Column(name = "inspection_started_at")
    private LocalDateTime inspectionStartedAt;

    @Column(name = "actual_failure", columnDefinition = "text")
    private String actualFailure;

    @Column(name = "root_cause", columnDefinition = "text")
    private String rootCause;

    @Column(name = "work_minutes")
    private Integer workMinutes;

    @Column(name = "tests_performed", columnDefinition = "text")
    private String testsPerformed;

    @Enumerated(EnumType.STRING)
    @Column(name = "final_equipment_state", length = 40)
    private EquipmentFinalState finalEquipmentState;

    @Column(columnDefinition = "text")
    private String recommendations;

    @Column(name = "pending_tasks", columnDefinition = "text")
    private String pendingTasks;

    @Column(name = "received_by", length = 160)
    private String receivedBy;

    @ElementCollection
    @CollectionTable(name = "maintenance_history_parts", joinColumns = @JoinColumn(name = "maintenance_history_id"))
    private List<MaintenancePart> parts = new ArrayList<>();

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