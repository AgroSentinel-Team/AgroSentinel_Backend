package com.agrosentinel.agrosentinel.services;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.agrosentinel.agrosentinel.dtos.UserOptionResponse;
import com.agrosentinel.agrosentinel.repositories.UsuarioRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserDirectoryService {
    private final UsuarioRepository usuarioRepository;
    private final UserRoleService userRoleService;

    public List<UserOptionResponse> findAllOptions() {
        return usuarioRepository.findAll().stream()
                .filter(usuario -> !Boolean.FALSE.equals(usuario.getActivo()))
                .map(usuario -> new UserOptionResponse(
                        usuario.getId(), usuario.getNombre(), usuario.getRol(),
                        userRoleService.getEffectiveRoles(usuario)))
                .sorted(Comparator.comparing(UserOptionResponse::nombre, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }
}