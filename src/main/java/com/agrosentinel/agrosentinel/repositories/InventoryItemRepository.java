package com.agrosentinel.agrosentinel.repositories;

import java.util.Optional;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.agrosentinel.agrosentinel.models.InventoryItem;

import jakarta.persistence.LockModeType;

public interface InventoryItemRepository extends JpaRepository<InventoryItem, Long> {
    List<InventoryItem> findAllByWorkGroup_IdOrderByNameAsc(Long groupId);

    List<InventoryItem> findAllByWorkGroupIsNullOrderByNameAsc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select item from InventoryItem item where item.id = :id")
    Optional<InventoryItem> findByIdForUpdate(@Param("id") Long id);
}
