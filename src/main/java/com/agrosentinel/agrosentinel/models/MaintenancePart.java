package com.agrosentinel.agrosentinel.models;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Embeddable
public class MaintenancePart {
    @Column(name = "part_number", nullable = false, length = 120)
    private String partNumber;

    @Column(name = "part_name", nullable = false, length = 160)
    private String name;

    @Column(name = "part_brand", length = 120)
    private String brand;

    @Column(name = "part_quantity", nullable = false, precision = 14, scale = 3)
    private BigDecimal quantity;
}
