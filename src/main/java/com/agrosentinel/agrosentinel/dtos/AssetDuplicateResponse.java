package com.agrosentinel.agrosentinel.dtos;

import java.util.List;

/** Activos que ya usan el serial o el código de inventario consultado. */
public record AssetDuplicateResponse(List<Match> serialNumberMatches, List<Match> inventoryCodeMatches) {
    public record Match(Long id, String name, String serialNumber, String inventoryCode) {
    }
}
