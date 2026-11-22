package com.agrosentinel.agrosentinel.models;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Configuración institucional de los informes impresos. Siempre hay una sola fila (id = 1). */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "report_settings")
public class ReportSettings {
    public static final short SINGLE_ID = 1;

    @Id
    private Short id = SINGLE_ID;

    @Column(name = "settings_json", nullable = false, columnDefinition = "text")
    private String settingsJson;

    @Column(name = "updated_by")
    private String updatedBy;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
