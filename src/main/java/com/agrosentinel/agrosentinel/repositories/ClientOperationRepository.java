package com.agrosentinel.agrosentinel.repositories;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.agrosentinel.agrosentinel.models.ClientOperation;

public interface ClientOperationRepository extends JpaRepository<ClientOperation, Long> {
    Optional<ClientOperation> findByUserEmailAndIdempotencyKey(String userEmail, String idempotencyKey);

    @Modifying
    @Transactional
    @Query("delete from ClientOperation operation where operation.createdAt < :before")
    int deleteCreatedBefore(@Param("before") Instant before);
}
