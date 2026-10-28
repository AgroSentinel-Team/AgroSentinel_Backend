package com.agrosentinel.agrosentinel.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.jupiter.api.Test;

import com.agrosentinel.agrosentinel.models.Usuario;
import com.agrosentinel.agrosentinel.repositories.WorkGroupRepository;

class UserRoleServiceTest {
    @Test
    void combinesIndividualAndActiveGroupRoles() {
        WorkGroupRepository workGroupRepository = mock(WorkGroupRepository.class);
        Usuario user = new Usuario();
        user.setId(12L);
        user.setRol("OPERATIVO_CAMPO");
        when(workGroupRepository.findActiveGrantedRolesByMemberId(12L)).thenReturn(Set.of("TECNICO"));

        assertEquals(Set.of("OPERATIVO_CAMPO", "TECNICO"),
                new UserRoleService(workGroupRepository).getEffectiveRoles(user));
    }

    @Test
    void normalizesRolePrefixAndCaseBeforeReturningAuthorities() {
        WorkGroupRepository workGroupRepository = mock(WorkGroupRepository.class);
        Usuario user = new Usuario();
        user.setId(13L);
        user.setRol("role_operativo_campo");
        when(workGroupRepository.findActiveGrantedRolesByMemberId(13L)).thenReturn(Set.of("role_tecnico"));

        assertEquals(Set.of("OPERATIVO_CAMPO", "TECNICO"),
                new UserRoleService(workGroupRepository).getEffectiveRoles(user));
    }
}
