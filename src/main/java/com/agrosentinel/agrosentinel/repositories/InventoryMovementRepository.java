package com.agrosentinel.agrosentinel.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.agrosentinel.agrosentinel.models.InventoryMovement;

public interface InventoryMovementRepository extends JpaRepository<InventoryMovement, Long> {
    @Query("select movement from InventoryMovement movement join fetch movement.item "
            + "order by movement.fecha desc, movement.id desc")
    List<InventoryMovement> findAllForHistory();
}
