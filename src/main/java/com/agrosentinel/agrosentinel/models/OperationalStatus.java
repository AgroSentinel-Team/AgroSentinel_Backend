package com.agrosentinel.agrosentinel.models;

public enum OperationalStatus {
    OPERATIONAL,
    OPERATIONAL_WITH_RESTRICTIONS,
    IN_REPAIR,
    /** Sin funcionar y sin reparación en curso (pendiente de evaluación, recuperación o baja). */
    OUT_OF_SERVICE,
    OBSOLETE
}
