package com.agrosentinel.agrosentinel.services;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

import com.agrosentinel.agrosentinel.models.Usuario;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
public class JwtService {
    private static final String ALGORITHM = "HmacSHA256";
    private final ObjectMapper objectMapper;
    private final byte[] secret;
    private final long tokenLifetimeSeconds;

    public JwtService(
            ObjectMapper objectMapper,
            @Value("${agrosentinel.security.jwt-secret}") String secret,
            @Value("${agrosentinel.security.jwt-lifetime-seconds:1800}") long tokenLifetimeSeconds) {
        this.objectMapper = objectMapper;
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.tokenLifetimeSeconds = Math.max(1800, tokenLifetimeSeconds);
        if (this.secret.length < 32) {
            throw new IllegalArgumentException("JWT secret must contain at least 32 bytes");
        }
    }

    public String createToken(Usuario usuario) {
        try {
            String header = encode(objectMapper.writeValueAsBytes(Map.of("alg", "HS256", "typ", "JWT")));
            String payload = encode(objectMapper.writeValueAsBytes(Map.of(
                    "sub", usuario.getEmail(),
                    "role", usuario.getRol(),
                    "exp", Instant.now().getEpochSecond() + tokenLifetimeSeconds)));
            String unsignedToken = header + "." + payload;
            return unsignedToken + "." + encode(sign(unsignedToken));
        } catch (JacksonException exception) {
            throw new IllegalStateException("Could not create authentication token", exception);
        }
    }

    public Authentication getAuthentication(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) return null;

            String unsignedToken = parts[0] + "." + parts[1];
            byte[] providedSignature = Base64.getUrlDecoder().decode(parts[2]);
            if (!MessageDigest.isEqual(sign(unsignedToken), providedSignature)) return null;

            JsonNode header = objectMapper.readTree(Base64.getUrlDecoder().decode(parts[0]));
            JsonNode payload = objectMapper.readTree(Base64.getUrlDecoder().decode(parts[1]));
            JsonNode subject = payload.get("sub");
            JsonNode role = payload.get("role");
            if (!header.path("alg").asText().equals("HS256")
                    || subject == null || subject.asText().isBlank()
                    || role == null || role.asText().isBlank()
                    || payload.path("exp").asLong() <= Instant.now().getEpochSecond()) {
                return null;
            }

            String roleName = role.asText();
            String authority = roleName.startsWith("ROLE_") ? roleName : "ROLE_" + roleName;
            return new UsernamePasswordAuthenticationToken(
                    subject.asText(), null, List.of(new SimpleGrantedAuthority(authority)));
        } catch (JacksonException | IllegalArgumentException | IllegalStateException exception) {
            return null;
        }
    }

    private byte[] sign(String value) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(secret, ALGORITHM));
            return mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Could not sign authentication token", exception);
        }
    }

    private String encode(byte[] value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }
}