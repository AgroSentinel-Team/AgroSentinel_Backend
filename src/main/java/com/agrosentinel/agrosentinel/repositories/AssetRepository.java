package com.agrosentinel.agrosentinel.repositories;

import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;

import com.agrosentinel.agrosentinel.models.Asset;

public interface AssetRepository extends JpaRepository<Asset, Long> {
	@Query("select a from Asset a where a.hasMotor = true and a.isActive = true")
	List<Asset> findActiveMotorAssets();

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select asset from Asset asset where asset.id = :id")
	Optional<Asset> findByIdForUpdate(@Param("id") Long id);
}