package com.agrosentinel.agrosentinel.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.agrosentinel.agrosentinel.models.AssetDiagnosis;

public interface AssetDiagnosisRepository extends JpaRepository<AssetDiagnosis, Long> {
    List<AssetDiagnosis> findAllByAssetIdOrderByDiagnosedOnDescIdDesc(Long assetId);

    /** Todos los diagnósticos de activos vigentes, del más reciente al más antiguo por activo. */
    @Query("select d from AssetDiagnosis d join fetch d.asset a where a.deletedAt is null order by a.id asc, d.diagnosedOn desc, d.id desc")
    List<AssetDiagnosis> findAllNewestFirstPerAsset();
}
