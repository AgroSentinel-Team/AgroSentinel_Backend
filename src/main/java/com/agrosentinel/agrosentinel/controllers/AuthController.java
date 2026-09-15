package com.agrosentinel.agrosentinel.controllers;

import java.util.HashMap;
import java.util.Map;

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

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:5173")
public class AuthController {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // ==========================================
    // 1. ENDPOINT DE REGISTRO
    // ==========================================
    @PostMapping("/registro")
    public ResponseEntity<?> registrarUsuario(@RequestBody Usuario usuario) {
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
        
        // Por ahora simularemos un token, luego implementamos la librería JWT real
        String tokenSimulado = "jwt_temporal_valido_para_" + usuario.getEmail();
        
        // Armamos la respuesta JSON que espera el Frontend
        Map<String, String> response = new HashMap<>();
        response.put("token", tokenSimulado);
        response.put("rol", usuario.getRol()); // Ejemplo: "TECNICO"
        
        return ResponseEntity.ok(response);
    }
}