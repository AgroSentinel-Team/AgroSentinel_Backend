package com.agrosentinel.agrosentinel.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

import com.agrosentinel.agrosentinel.models.Asset;

public interface AssetRepository extends JpaRepository<Asset, Long> {
	@Query("select a from Asset a where a.hasMotor = true and a.isActive = true")
	List<Asset> findActiveMotorAssets();
}