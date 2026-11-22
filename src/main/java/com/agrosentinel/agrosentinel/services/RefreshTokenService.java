package com.agrosentinel.agrosentinel.services;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.agrosentinel.agrosentinel.models.RefreshToken;
import com.agrosentinel.agrosentinel.models.Usuario;
import com.agrosentinel.agrosentinel.repositories.RefreshTokenRepository;

/**
 * Emite y rota tokens de renovación de larga duración.
 *
 * <p>Cada renovación invalida el token usado y entrega uno nuevo con la vigencia completa,
 * así la sesión se mantiene mientras el dispositivo se conecte al menos una vez dentro del plazo.
 * Un token ya rotado se acepta durante un periodo de gracia corto para tolerar respuestas
 * perdidas por mala señal en campo.</p>
 */
@Service
public class RefreshTokenService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String INVALID_MESSAGE = "La sesión guardada ya no es válida. Inicia sesión de nuevo.";

    private final RefreshTokenRepository repository;
    private final Duration lifetime;
    private final Duration rotationGrace;
    private final Clock clock;

    @Autowired
    public RefreshTokenService(
            RefreshTokenRepository repository,
            @Value("${agrosentinel.security.refresh-token-lifetime-days:14}") long lifetimeDays,
            @Value("${agrosentinel.security.refresh-token-rotation-grace-seconds:300}") long graceSeconds) {
        this(repository, Duration.ofDays(Math.max(1, lifetimeDays)), Duration.ofSeconds(Math.max(0, graceSeconds)),
                Clock.systemUTC());
    }

    RefreshTokenService(RefreshTokenRepository repository, Duration lifetime, Duration rotationGrace, Clock clock) {
        this.repository = repository;
        this.lifetime = lifetime;
        this.rotationGrace = rotationGrace;
        this.clock = clock;
    }

    public record IssuedToken(String value, Instant expiresAt, Usuario user) {
    }

    @Transactional
    public IssuedToken issue(Usuario user) {
        Instant now = clock.instant();
        repository.deleteExpiredForUser(user.getId(), now);

        String value = generateValue();
        RefreshToken token = new RefreshToken();
        token.setUser(user);
        token.setTokenHash(hash(value));
        token.setCreatedAt(now);
        token.setExpiresAt(now.plus(lifetime));
        repository.save(token);
        return new IssuedToken(value, token.getExpiresAt(), user);
    }

    /** Valida el token recibido, lo marca como usado y devuelve uno nuevo. */
    @Transactional
    public IssuedToken rotate(String presentedValue) {
        RefreshToken current = findUsable(presentedValue);
        Instant now = clock.instant();
        if (current.getRotatedAt() == null) {
            current.setRotatedAt(now);
            repository.save(current);
        }
        return issue(current.getUser());
    }

    @Transactional
    public void revoke(String presentedValue) {
        if (presentedValue == null || presentedValue.isBlank()) return;
        repository.findByTokenHashForUpdate(hash(presentedValue)).ifPresent(token -> {
            if (token.getRevokedAt() == null) {
                token.setRevokedAt(clock.instant());
                repository.save(token);
            }
        });
    }

    private RefreshToken findUsable(String presentedValue) {
        if (presentedValue == null || presentedValue.isBlank()) {
            throw invalid();
        }
        RefreshToken token = repository.findByTokenHashForUpdate(hash(presentedValue)).orElseThrow(this::invalid);
        Instant now = clock.instant();
        Usuario user = token.getUser();
        boolean expired = !token.getExpiresAt().isAfter(now);
        boolean revoked = token.getRevokedAt() != null;
        boolean rotatedOutsideGrace = token.getRotatedAt() != null
                && token.getRotatedAt().plus(rotationGrace).isBefore(now);
        boolean inactiveUser = user == null || Boolean.FALSE.equals(user.getActivo());
        if (expired || revoked || rotatedOutsideGrace || inactiveUser) {
            throw invalid();
        }
        return token;
    }

    private ResponseStatusException invalid() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, INVALID_MESSAGE);
    }

    private static String generateValue() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String hash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 no disponible", exception);
        }
    }
}
