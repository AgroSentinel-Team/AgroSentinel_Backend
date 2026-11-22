package com.agrosentinel.agrosentinel.models;

/** Situación del código de inventario institucional del activo. */
public enum InventoryCodeStatus {
    /** El activo tiene código registrado. */
    ASSIGNED,
    /** El activo aún no tiene código: falta asignarlo. */
    PENDING,
    /** El código existe pero no se suministró al registrar el activo. */
    NOT_PROVIDED
}
