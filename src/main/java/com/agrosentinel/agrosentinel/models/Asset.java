package com.agrosentinel.agrosentinel.models;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.SQLRestriction;

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
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "assets")
@SQLRestriction("deleted_at IS NULL")
public class Asset {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String brand;

    private String model;

    // No es único: los inventarios reales traen seriales repetidos y la aplicación solo advierte.
    @Column(name = "serial_number")
    private String serialNumber;

    @Column(name = "inventory_code", length = 80)
    private String inventoryCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "inventory_code_status", nullable = false, length = 20)
    private InventoryCodeStatus inventoryCodeStatus = InventoryCodeStatus.ASSIGNED;

    /** Tipo concreto de equipo dentro de la categoría, p. ej. "Guadaña" o "Rastra transportable". */
    @Column(name = "equipment_type", length = 120)
    private String equipmentType;

    @Column(name = "manufacture_year")
    private Integer manufactureYear;

    @Column(name = "extra_specifications", columnDefinition = "text")
    private String extraSpecifications;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "responsible_id", nullable = false)
    private Usuario responsible;

    @Column(name = "acquisition_date")
    private LocalDate acquisitionDate;

    @Column(name = "acquisition_cost", precision = 12, scale = 2)
    private BigDecimal acquisitionCost;

    private String provider;

    @Column(name = "warranty_expiration_date")
    private LocalDate warrantyExpirationDate;

    private String location;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "current_horometer", nullable = false, precision = 12, scale = 2)
    private BigDecimal currentHorometer = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "operational_status", nullable = false)
    private OperationalStatus operationalStatus = OperationalStatus.OPERATIONAL;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "has_motor", nullable = false, columnDefinition = "boolean not null default false")
    private boolean hasMotor = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @OneToMany(mappedBy = "asset", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC, id ASC")
    @BatchSize(size = 64)
    private List<AssetSpecification> specifications = new ArrayList<>();

    @PrePersist
    private void applyDefaults() {
        if (operationalStatus == null) {
            operationalStatus = OperationalStatus.OPERATIONAL;
        }
        if (inventoryCodeStatus == null) {
            inventoryCodeStatus = InventoryCodeStatus.ASSIGNED;
        }
    }
}