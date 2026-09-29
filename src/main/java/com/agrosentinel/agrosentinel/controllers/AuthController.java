package com.agrosentinel.agrosentinel.controllers;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.agrosentinel.agrosentinel.models.Usuario;
import com.agrosentinel.agrosentinel.repositories.UsuarioRepository;
import com.agrosentinel.agrosentinel.services.JwtService;

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

    // ==========================================
    // 1. ENDPOINT DE REGISTRO
    // ==========================================
    @PostMapping("/registro")
    public ResponseEntity<?> registrarUsuario(@RequestBody Usuario usuario) {
        if (usuario.getRol() == null || !SELF_REGISTERABLE_ROLES.contains(usuario.getRol())) {
            return ResponseEntity.status(403).body("El rol Administrador debe asignarse fuera del autorregistro.");
        }
        if(usuarioRepository.existsByEmail(usuario.getEmail())) {
            return ResponseEntity.badRequest().body("Error: El email ya está en uso");
        }
        
        // ¡Magia! Encriptamos la contraseña antes de guardarla en Supabase
        usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));
        usuarioRepository.save(usuario);
        
        return ResponseEntity.ok("Usuario registrado con éxito");
    }

    // ==========================================
    // 2. ENDPOINT DE LOGIN
    // ==========================================
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Usuario loginRequest) {
        // Buscamos si el correo existe
        Usuario usuario = usuarioRepository.findByEmail(loginRequest.getEmail());
        
        // Verificamos si existe y si la contraseña coincide con la encriptada
        if (usuario == null || !passwordEncoder.matches(loginRequest.getPassword(), usuario.getPassword())) {
            return ResponseEntity.status(401).body("Error: Credenciales incorrectas");
        }
        
        String token = jwtService.createToken(usuario);
        
        // Armamos la respuesta JSON que espera el Frontend
        Map<String, String> response = new HashMap<>();
        response.put("token", token);
        response.put("rol", usuario.getRol()); // Ejemplo: "TECNICO"
        
        return ResponseEntity.ok(response);
    }
}