package com.agrosentinel.agrosentinel.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.agrosentinel.agrosentinel.models.InventoryMovement;

public interface InventoryMovementRepository extends JpaRepository<InventoryMovement, Long> {
    @Query("select movement from InventoryMovement movement join fetch movement.item "
            + "order by movement.fecha desc, movement.id desc")
    List<InventoryMovement> findAllForHistory();

    @Query("select movement from InventoryMovement movement join fetch movement.item item "
            + "where item.workGroup.id = :groupId order by movement.fecha desc, movement.id desc")
    List<InventoryMovement> findAllForGroupHistory(@org.springframework.data.repository.query.Param("groupId") Long groupId);

    @Query("select movement from InventoryMovement movement join fetch movement.item item "
            + "where item.workGroup is null order by movement.fecha desc, movement.id desc")
    List<InventoryMovement> findAllForCentralHistory();
}
