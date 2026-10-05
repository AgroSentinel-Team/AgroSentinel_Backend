package com.agrosentinel.agrosentinel.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.agrosentinel.agrosentinel.models.MaintenanceHistory;

public interface MaintenanceHistoryRepository extends JpaRepository<MaintenanceHistory, Long> {
    List<MaintenanceHistory> findAllByAssetIdOrderByMaintenanceDateDesc(Long assetId);

    java.util.Optional<MaintenanceHistory> findByPreventiveMaintenanceId(Long preventiveMaintenanceId);

    java.util.Optional<MaintenanceHistory> findByFailureReportId(Long failureReportId);

    @Query("select history from MaintenanceHistory history "
            + "left join history.preventiveMaintenance preventive "
            + "left join history.failureReport failure "
            + "where history.executedAt is not null "
            + "and (preventive.technician.id = :technicianId "
            + "or failure.assignedTechnician.id = :technicianId) "
            + "order by history.executedAt desc, history.id desc")
    List<MaintenanceHistory> findAllExecutedByTechnicianId(@Param("technicianId") Long technicianId);
}