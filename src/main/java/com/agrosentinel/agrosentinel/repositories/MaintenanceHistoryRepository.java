package com.agrosentinel.agrosentinel.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.agrosentinel.agrosentinel.models.MaintenanceHistory;

public interface MaintenanceHistoryRepository extends JpaRepository<MaintenanceHistory, Long> {
    List<MaintenanceHistory> findAllByAssetIdOrderByMaintenanceDateDesc(Long assetId);

    java.util.Optional<MaintenanceHistory> findByPreventiveMaintenanceId(Long preventiveMaintenanceId);
}