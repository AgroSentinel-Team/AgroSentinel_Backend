package com.agrosentinel.agrosentinel.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.agrosentinel.agrosentinel.models.ReportSettings;

public interface ReportSettingsRepository extends JpaRepository<ReportSettings, Short> {
}
