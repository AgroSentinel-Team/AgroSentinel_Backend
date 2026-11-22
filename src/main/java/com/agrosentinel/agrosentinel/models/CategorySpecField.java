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

/** Campo de especificación que se pide para los activos de una categoría. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "category_spec_templates")
public class CategorySpecField {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(name = "spec_key", nullable = false, length = 60)
    private String specKey;

    @Column(nullable = false, length = 120)
    private String label;

    @Enumerated(EnumType.STRING)
    @Column(name = "data_type", nullable = false, length = 20)
    private SpecDataType dataType = SpecDataType.TEXT;

    @Column(length = 20)
    private String unit;

    /** Opciones separadas por salto de línea (solo para OPTION). */
    @Column(columnDefinition = "text")
    private String options;

    @Column(nullable = false)
    private boolean required;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "help_text", length = 255)
    private String helpText;
}
