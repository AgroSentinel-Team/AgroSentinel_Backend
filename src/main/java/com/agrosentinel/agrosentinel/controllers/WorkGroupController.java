package com.agrosentinel.agrosentinel.controllers;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.agrosentinel.agrosentinel.dtos.WorkGroupRequest;
import com.agrosentinel.agrosentinel.dtos.WorkGroupResponse;
import com.agrosentinel.agrosentinel.dtos.WorkGroupRolesRequest;
import com.agrosentinel.agrosentinel.services.WorkGroupService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/work-groups")
@RequiredArgsConstructor
public class WorkGroupController {
    private final WorkGroupService workGroupService;

    @GetMapping
    public List<WorkGroupResponse> findAll() {
        return workGroupService.findAll();
    }

    @GetMapping("/options")
    public List<WorkGroupResponse> findOptions(
            @RequestParam(name = "all", defaultValue = "false") boolean all,
            Authentication authentication) {
        boolean isAdministrator = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMINISTRADOR")
                        || authority.getAuthority().equals("ROLE_ADMIN"));
        boolean isTechnician = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_TECNICO"));
        if (all && !isAdministrator && !isTechnician) {
            throw new AccessDeniedException("No tienes permiso para consultar todos los grupos");
        }
        String username = authentication != null ? authentication.getName() : "";
        return isAdministrator || (all && isTechnician)
                ? workGroupService.findAll()
                : workGroupService.findForMember(username);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WorkGroupResponse create(@Valid @RequestBody WorkGroupRequest request) {
        return workGroupService.create(request);
    }

    @PutMapping("/{id}")
    public WorkGroupResponse update(@PathVariable Long id, @Valid @RequestBody WorkGroupRequest request) {
        return workGroupService.update(id, request);
    }

    @PostMapping("/{id}/members/{userId}")
    public WorkGroupResponse addMember(@PathVariable Long id, @PathVariable Long userId) {
        return workGroupService.addMember(id, userId);
    }

    @DeleteMapping("/{id}/members/{userId}")
    public WorkGroupResponse removeMember(@PathVariable Long id, @PathVariable Long userId) {
        return workGroupService.removeMember(id, userId);
    }

    @PatchMapping("/{id}/roles")
    public WorkGroupResponse updateRoles(
            @PathVariable Long id,
            @Valid @RequestBody WorkGroupRolesRequest request) {
        return workGroupService.updateRoles(id, request.roles());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        workGroupService.softDelete(id);
    }
}
