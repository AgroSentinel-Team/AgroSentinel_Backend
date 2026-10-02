package com.agrosentinel.agrosentinel.models;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "preventive_maintenances")
public class PreventiveMaintenance {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asset_id", nullable = false)
    private Asset asset;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "technician_id", nullable = false)
    private Usuario technician;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "executed_by_id")
    private Usuario executedBy;

    @Column(name = "scheduled_at", nullable = false)
    private LocalDateTime scheduledAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "attention_index", nullable = false, length = 16, columnDefinition = "varchar(16) not null default 'LOW'")
    private MaintenanceAttentionIndex attentionIndex = MaintenanceAttentionIndex.LOW;

    @Column(name = "predictive_context", columnDefinition = "text")
    private String predictiveContext;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PreventiveMaintenanceStatus status = PreventiveMaintenanceStatus.SCHEDULED;

    @Column(name = "executed_at")
    private LocalDateTime executedAt;

    @Column(name = "replaced_parts", columnDefinition = "text")
    private String replacedParts;

    @Column(name = "work_description", columnDefinition = "text")
    private String workDescription;

    @Column(name = "technician_notes", columnDefinition = "text")
    private String technicianNotes;

    @Column(name = "next_maintenance_date")
    private LocalDate nextMaintenanceDate;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @OneToMany(mappedBy = "maintenance", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PreventiveMaintenanceTask> tasks = new ArrayList<>();

    @OneToMany(mappedBy = "maintenance", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PreventiveMaintenanceEvidence> evidences = new ArrayList<>();
}
