package com.agrosentinel.agrosentinel.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.agrosentinel.agrosentinel.models.Asset;

public interface AssetRepository extends JpaRepository<Asset, Long> {
}