package com.agrosentinel.agrosentinel.repositories;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.agrosentinel.agrosentinel.models.WorkGroup;

import jakarta.persistence.LockModeType;

public interface WorkGroupRepository extends JpaRepository<WorkGroup, Long> {
    List<WorkGroup> findAllByDeletedAtIsNullOrderByNameAsc();

    List<WorkGroup> findAllByMembers_IdAndDeletedAtIsNullOrderByNameAsc(Long memberId);

    Optional<WorkGroup> findByIdAndDeletedAtIsNull(Long id);

    Optional<WorkGroup> findByNameIgnoreCaseAndDeletedAtIsNull(String name);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select workGroup from WorkGroup workGroup "
            + "where workGroup.id = :id and workGroup.deletedAt is null")
    Optional<WorkGroup> findActiveByIdForUpdate(@Param("id") Long id);

    @Query("select distinct grantedRole from WorkGroup workGroup join workGroup.members member "
            + "join workGroup.grantedRoles grantedRole where member.id = :memberId "
            + "and workGroup.deletedAt is null")
    Set<String> findActiveGrantedRolesByMemberId(@Param("memberId") Long memberId);
}
