package com.agrosentinel.agrosentinel.controllers;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.agrosentinel.agrosentinel.dtos.MaintenanceHistoryResponse;
import com.agrosentinel.agrosentinel.models.Usuario;
import com.agrosentinel.agrosentinel.repositories.UsuarioRepository;
import com.agrosentinel.agrosentinel.services.AssetService;
import com.agrosentinel.agrosentinel.services.UserRoleService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/maintenance-history")
@RequiredArgsConstructor
public class MaintenanceHistoryController {
    private final AssetService assetService;
    private final UsuarioRepository usuarioRepository;
    private final UserRoleService userRoleService;

    @GetMapping("/technician")
    public List<MaintenanceHistoryResponse> findTechnicianHistory(Authentication authentication) {
        Usuario technician = usuarioRepository.findByEmail(authentication.getName());
        if (technician == null || Boolean.FALSE.equals(technician.getActivo())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario autenticado no disponible");
        }
        if (!userRoleService.getEffectiveRoles(technician).contains("TECNICO")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Esta consulta es exclusiva para técnicos");
        }
        return assetService.getTechnicianHistory(technician.getId());
    }
}
