package com.agrosentinel.agrosentinel.dtos;

import jakarta.validation.constraints.Size;

/**
 * Valor de una especificación. Si specKey coincide con un campo de la plantilla de la categoría
 * se usan su etiqueta, tipo y unidad; si no, se guarda como campo adicional con la etiqueta dada.
 */
public record AssetSpecificationRequest(
        @Size(max = 60) String specKey,
        @Size(max = 120) String label,
        @Size(max = 500) String value,
        @Size(max = 20) String unit) {
}
