package com.agrosentinel.agrosentinel.dtos;

public record TechnicianAvailabilityResponse(
        Long id,
        String nombre,
        long activeWorkload,
        int workloadCapacity,
        boolean available) {
}
