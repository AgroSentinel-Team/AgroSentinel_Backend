package com.agrosentinel.agrosentinel.controllers;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
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

    @Autowired
    private JwtEncoder jwtEncoder;

    // ==========================================
    // 1. ENDPOINT DE REGISTRO
    // ==========================================
    @PostMapping("/registro")
    public ResponseEntity<?> registrarUsuario(@RequestBody Usuario usuario) {
        if ("ADMINISTRADOR".equals(usuario.getRol())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("El rol administrador solo puede asignarlo un administrador autenticado.");
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
        if (usuario == null || !Boolean.TRUE.equals(usuario.getActivo()) && usuario.getActivo() != null
            || !passwordEncoder.matches(loginRequest.getPassword(), usuario.getPassword())) {
            return ResponseEntity.status(401).body("Error: Credenciales incorrectas");
        }

        Instant issuedAt = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
            .issuer("agrosentinel")
            .issuedAt(issuedAt)
            .expiresAt(issuedAt.plus(Duration.ofHours(8)))
            .subject(usuario.getEmail())
            .claim("rol", usuario.getRol())
            .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();

        usuario.setUltimoAcceso(LocalDateTime.now());
        usuarioRepository.save(usuario);

        Map<String, String> response = Map.of("token", token, "rol", usuario.getRol());
        
        return ResponseEntity.ok(response);
    }
}