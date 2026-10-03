package com.agrosentinel.agrosentinel.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.agrosentinel.agrosentinel.models.InventoryTask;

public interface InventoryTaskRepository extends JpaRepository<InventoryTask, Long> {
    List<InventoryTask> findAllByOrderByCompletedAscCreatedAtDesc();
}
