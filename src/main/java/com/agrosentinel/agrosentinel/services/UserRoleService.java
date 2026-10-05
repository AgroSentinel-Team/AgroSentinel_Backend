package com.agrosentinel.agrosentinel.services;

import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.agrosentinel.agrosentinel.models.Usuario;
import com.agrosentinel.agrosentinel.repositories.WorkGroupRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserRoleService {
    private final WorkGroupRepository workGroupRepository;

    public Set<String> getEffectiveRoles(Usuario user) {
        Set<String> roles = new LinkedHashSet<>();
        addRole(roles, user.getRol());
        roles.addAll(workGroupRepository.findActiveGrantedRolesByMemberId(user.getId()));
        return Set.copyOf(roles);
    }

    private void addRole(Set<String> roles, String role) {
        if (role != null && !role.isBlank()) {
            roles.add(role.startsWith("ROLE_") ? role.substring(5) : role);
        }
    }
}
