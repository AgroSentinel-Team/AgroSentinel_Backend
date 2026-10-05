package com.agrosentinel.agrosentinel.dtos;

import java.util.List;
import java.util.Set;

public record WorkGroupResponse(
        Long id,
        String name,
        String description,
        Long leaderId,
        String leaderName,
        List<WorkGroupMemberResponse> members,
        Set<String> grantedRoles,
        boolean active) {
}
