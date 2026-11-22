package com.agrosentinel.agrosentinel.models;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

/**
 * Dato técnico de un activo (potencia, cilindrada, número de discos...). Guarda la etiqueta y
 * la unidad con las que se registró, para que la ficha se pueda imprimir aunque la plantilla
 * de la categoría cambie después.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "asset_specifications")
public class AssetSpecification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asset_id", nullable = false)
    private Asset asset;

    @Column(name = "spec_key", nullable = false, length = 60)
    private String specKey;

    @Column(nullable = false, length = 120)
    private String label;

    @Column(name = "value_text", length = 500)
    private String valueText;

    @Column(name = "value_number", precision = 14, scale = 3)
    private BigDecimal valueNumber;

    @Column(length = 20)
    private String unit;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;
}
