package com.agrosentinel.agrosentinel.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.agrosentinel.agrosentinel.models.PredictiveRecord;

public interface PredictiveRecordRepository extends JpaRepository<PredictiveRecord, Long> {
    List<PredictiveRecord> findAllByAssetIdOrderByTimestampDesc(Long assetId);

    Optional<PredictiveRecord> findFirstByAssetIdOrderByTimestampDesc(Long assetId);
}