package com.agrosentinel.agrosentinel.repositories;

import java.util.List;
import java.util.Collection;
import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.agrosentinel.agrosentinel.models.FailureReport;
import com.agrosentinel.agrosentinel.models.FailureStatus;

public interface FailureReportRepository extends JpaRepository<FailureReport, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select report from FailureReport report where report.id = :id")
    Optional<FailureReport> findByIdForUpdate(@Param("id") Long id);

    List<FailureReport> findAllByOrderByFechaReporteDesc();

    List<FailureReport> findAllByUserIdOrderByFechaReporteDesc(Long userId);

    List<FailureReport> findAllByAssignedTechnicianIdOrderByFechaReporteDesc(Long technicianId);

    long countByAssignedTechnicianIdAndEstadoIn(Long technicianId, Collection<FailureStatus> statuses);
}
