package com.agrosentinel.agrosentinel.models;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.hibernate.annotations.SQLRestriction;

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

    @Column(name = "serial_number", unique = true)
    private String serialNumber;

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

    @Enumerated(EnumType.STRING)
    @Column(name = "operational_status", nullable = false)
    private OperationalStatus operationalStatus = OperationalStatus.OPERATIONAL;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @PrePersist
    private void applyDefaults() {
        if (operationalStatus == null) {
            operationalStatus = OperationalStatus.OPERATIONAL;
        }
    }
}