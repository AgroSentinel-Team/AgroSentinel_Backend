package com.agrosentinel.agrosentinel.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.agrosentinel.agrosentinel.dtos.WorkGroupRequest;
import com.agrosentinel.agrosentinel.dtos.WorkGroupResponse;
import com.agrosentinel.agrosentinel.models.Usuario;
import com.agrosentinel.agrosentinel.models.WorkGroup;
import com.agrosentinel.agrosentinel.repositories.UsuarioRepository;
import com.agrosentinel.agrosentinel.repositories.WorkGroupRepository;

class WorkGroupServiceTest {
    @Test
    void creationIncludesLeaderAndNormalizesGroupRoles() {
        WorkGroupRepository workGroupRepository = mock(WorkGroupRepository.class);
        UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
        Usuario leader = user(1L, "líder@example.com");
        Usuario member = user(2L, "miembro@example.com");
        when(workGroupRepository.findByNameIgnoreCaseAndDeletedAtIsNull("Zona norte"))
                .thenReturn(Optional.empty());
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(leader));
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(member));
        when(workGroupRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        WorkGroupResponse response = new WorkGroupService(
                workGroupRepository, usuarioRepository, mock(UserRoleService.class))
                .create(new WorkGroupRequest("Zona norte", "Equipo norte", 1L, Set.of(2L), Set.of("role_tecnico")));

        assertEquals(2, response.members().size());
        assertEquals(1L, response.leaderId());
        assertEquals(Set.of("TECNICO"), response.grantedRoles());
    }

    @Test
    void groupsCannotGrantAdministrativeRoles() {
        WorkGroupRepository workGroupRepository = mock(WorkGroupRepository.class);
        when(workGroupRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(new WorkGroup()));
        WorkGroupService service = new WorkGroupService(
                workGroupRepository, mock(UsuarioRepository.class), mock(UserRoleService.class));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.updateRoles(1L, Set.of("ADMINISTRADOR")));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    private Usuario user(Long id, String email) {
        Usuario user = new Usuario();
        user.setId(id);
        user.setEmail(email);
        user.setNombre(email);
        user.setRol("OPERATIVO_CAMPO");
        user.setActivo(true);
        return user;
    }
}
