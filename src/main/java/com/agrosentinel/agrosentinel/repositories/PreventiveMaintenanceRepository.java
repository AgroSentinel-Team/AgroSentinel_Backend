package com.agrosentinel.agrosentinel.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.agrosentinel.agrosentinel.models.PreventiveMaintenance;
import com.agrosentinel.agrosentinel.models.PreventiveMaintenanceStatus;

public interface PreventiveMaintenanceRepository extends JpaRepository<PreventiveMaintenance, Long> {
    List<PreventiveMaintenance> findAllByOrderByScheduledAtAsc();

    List<PreventiveMaintenance> findAllByTechnician_EmailOrderByScheduledAtAsc(String email);

    List<PreventiveMaintenance> findAllByAssetIdAndStatusOrderByScheduledAtAsc(
            Long assetId, PreventiveMaintenanceStatus status);
}
