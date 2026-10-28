package com.agrosentinel.agrosentinel.controllers;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import com.agrosentinel.agrosentinel.dtos.WorkGroupRolesRequest;
import com.agrosentinel.agrosentinel.services.WorkGroupService;

class WorkGroupControllerTest {
    private final WorkGroupService workGroupService = mock(WorkGroupService.class);
    private final WorkGroupController controller = new WorkGroupController(workGroupService);

    @Test
    void onlyTechniciansAndAdministratorsCanRequestAllGroupOptions() {
        var supervisor = authentication("supervisor@example.com", "ROLE_SUPERVISOR");

        assertThrows(AccessDeniedException.class, () -> controller.findOptions(true, supervisor));
    }

    @Test
    void technicianCanRequestAllGroupOptions() {
        var technician = authentication("technician@example.com", "ROLE_TECNICO");
        when(workGroupService.findAll()).thenReturn(List.of());

        controller.findOptions(true, technician);

        verify(workGroupService).findAll();
    }

    @Test
    void roleUpdateUsesTheExistingGroupRoleService() {
        var roles = Set.of("TECNICO");

        controller.updateRoles(3L, new WorkGroupRolesRequest(roles));

        verify(workGroupService).updateRoles(3L, roles);
    }

    @Test
    void deletingAGroupUsesSoftDelete() {
        controller.delete(3L);

        verify(workGroupService).softDelete(3L);
    }

    private UsernamePasswordAuthenticationToken authentication(String email, String role) {
        return new UsernamePasswordAuthenticationToken(
                email, null, List.of(new SimpleGrantedAuthority(role)));
    }
}
