package com.agrosentinel.agrosentinel.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.Base64;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;

import com.agrosentinel.agrosentinel.models.Usuario;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

class JwtServiceTest {
    @Test
    void issuedTokenIsValidAndLastsAtLeastOneHour() {
        JwtService jwtService = new JwtService(
                new ObjectMapper(), "test-jwt-secret-with-at-least-thirty-two-bytes", 1800);
        Usuario usuario = new Usuario();
        usuario.setId(42L);
        usuario.setEmail("test@example.com");
        usuario.setRol("ADMINISTRADOR");

        String token = jwtService.createToken(usuario);
        Authentication authentication = jwtService.getAuthentication(token);

        assertNotNull(authentication);
        assertEquals("test@example.com", authentication.getPrincipal());
        assertTrue(authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMINISTRADOR")));

        JsonNode claims = new ObjectMapper().readTree(Base64.getUrlDecoder().decode(token.split("\\.")[1]));
        long remainingSeconds = claims.path("exp").asLong() - Instant.now().getEpochSecond();
        assertTrue(remainingSeconds >= 3600);
    }
}