package com.agrosentinel.agrosentinel.controllers;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.agrosentinel.agrosentinel.models.Usuario;
import com.agrosentinel.agrosentinel.repositories.UsuarioRepository;
import com.agrosentinel.agrosentinel.services.JwtService;
import com.agrosentinel.agrosentinel.services.UserRoleService;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:5173")
public class AuthController {
    private static final Set<String> SELF_REGISTERABLE_ROLES = Set.of(
            "TECNICO", "SUPERVISOR", "ALMACENISTA", "OPERATIVO_CAMPO");

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRoleService userRoleService;

    @PostMapping("/registro")
    public ResponseEntity<?> registrarUsuario(@RequestBody Usuario usuario) {
        if (usuario.getRol() == null || !SELF_REGISTERABLE_ROLES.contains(usuario.getRol())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("El rol Administrador debe asignarse fuera del autorregistro.");
        }
        if (usuarioRepository.existsByEmail(usuario.getEmail())) {
            return ResponseEntity.badRequest().body("Error: El email ya está en uso");
        }

        usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));
        usuario.setActivo(true);
        usuarioRepository.save(usuario);
        return ResponseEntity.ok("Usuario registrado con éxito");
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Usuario loginRequest) {
        Usuario usuario = usuarioRepository.findByEmail(loginRequest.getEmail());
        if (usuario == null || Boolean.FALSE.equals(usuario.getActivo())
                || loginRequest.getPassword() == null
                || !passwordEncoder.matches(loginRequest.getPassword(), usuario.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Error: Credenciales incorrectas");
        }

        String token = jwtService.createToken(usuario);
        usuario.setUltimoAcceso(LocalDateTime.now());
        usuarioRepository.save(usuario);

        return ResponseEntity.ok(Map.of(
                "token", token,
                "rol", usuario.getRol(),
                "nombre", usuario.getNombre(),
                "roles", userRoleService.getEffectiveRoles(usuario)));
    }

    @GetMapping("/me")
    public ResponseEntity<?> currentUser(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Se requiere autenticación");
        }
        Usuario usuario = usuarioRepository.findByEmail(authentication.getName());
        if (usuario == null || Boolean.FALSE.equals(usuario.getActivo())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Usuario autenticado no disponible");
        }
        return ResponseEntity.ok(Map.of(
                "id", usuario.getId(),
                "nombre", usuario.getNombre(),
                "email", usuario.getEmail(),
                "rol", usuario.getRol(),
                "roles", userRoleService.getEffectiveRoles(usuario)));
    }
}