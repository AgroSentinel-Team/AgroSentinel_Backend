package com.agrosentinel.agrosentinel.dtos;

import java.util.Set;

public record WorkGroupMemberResponse(
        Long id,
        String nombre,
        String email,
        String rol,
        Set<String> roles,
        boolean lider) {
}
