package com.agrosentinel.agrosentinel.models;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "inventory_tasks")
public class InventoryTask {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 240)
    private String description;

    @Column(nullable = false)
    private boolean completed;

    @Column(nullable = false, length = 160)
    private String createdBy;

    @Column(nullable = false)
    private Instant createdAt;

    @Column
    private Instant completedAt;

    @PrePersist
    private void applyDefaults() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    @PreUpdate
    private void updateCompletionTime() {
        completedAt = completed ? Instant.now() : null;
    }
}
