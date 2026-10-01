package com.agrosentinel.agrosentinel.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.agrosentinel.agrosentinel.models.FailureReport;

public interface FailureReportRepository extends JpaRepository<FailureReport, Long> {
    List<FailureReport> findAllByOrderByFechaReporteDesc();

    List<FailureReport> findAllByUserIdOrderByFechaReporteDesc(Long userId);
}
