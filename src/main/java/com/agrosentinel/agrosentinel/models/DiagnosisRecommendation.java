package com.agrosentinel.agrosentinel.models;

/** Qué se recomienda hacer con el activo según su diagnóstico. */
public enum DiagnosisRecommendation {
    /** Conservar en operación (con o sin mantenimiento). */
    KEEP,
    /** Fuera de servicio, pero recuperable con reparación. */
    RECOVER,
    /** Hace falta evaluación técnica y económica antes de decidir. */
    PENDING_EVALUATION,
    /** Se recomienda evaluar formalmente la baja. */
    EVALUATE_DISPOSAL,
    /** Se recomienda la baja (requiere soporte técnico-económico). */
    RECOMMENDED_DISPOSAL
}
