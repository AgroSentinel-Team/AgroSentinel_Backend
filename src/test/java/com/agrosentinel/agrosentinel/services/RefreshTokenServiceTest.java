package com.agrosentinel.agrosentinel.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.agrosentinel.agrosentinel.models.RefreshToken;
import com.agrosentinel.agrosentinel.models.Usuario;
import com.agrosentinel.agrosentinel.repositories.RefreshTokenRepository;

class RefreshTokenServiceTest {
    private static final Instant START = Instant.parse("2026-11-21T12:00:00Z");

    private final Map<String, RefreshToken> stored = new HashMap<>();
    private MutableClock clock;
    private RefreshTokenService service;
    private Usuario technician;

    @BeforeEach
    void setUp() {
        RefreshTokenRepository repository = mock(RefreshTokenRepository.class);
        when(repository.save(any(RefreshToken.class))).thenAnswer(invocation -> {
            RefreshToken token = invocation.getArgument(0);
            stored.put(token.getTokenHash(), token);
            return token;
        });
        when(repository.findByTokenHashForUpdate(anyString()))
                .thenAnswer(invocation -> Optional.ofNullable(stored.get(invocation.<String>getArgument(0))));
        clock = new MutableClock(START);
        service = new RefreshTokenService(repository, Duration.ofDays(14), Duration.ofMinutes(5), clock);
        technician = new Usuario();
        technician.setId(3L);
        technician.setEmail("tecnico@agrosentinel.test");
        technician.setActivo(true);
    }

    @Test
    void issueStoresOnlyTheHashWithFullLifetime() {
        RefreshTokenService.IssuedToken issued = service.issue(technician);

        assertNotNull(issued.value());
        assertEquals(START.plus(Duration.ofDays(14)), issued.expiresAt());
        RefreshToken saved = stored.get(RefreshTokenService.hash(issued.value()));
        assertNotNull(saved);
        assertNotEquals(issued.value(), saved.getTokenHash());
    }

    @Test
    void rotateReturnsANewTokenAndExtendsTheSession() {
        RefreshTokenService.IssuedToken first = service.issue(technician);
        clock.advance(Duration.ofDays(10));

        RefreshTokenService.IssuedToken second = service.rotate(first.value());

        assertNotEquals(first.value(), second.value());
        assertEquals(START.plus(Duration.ofDays(24)), second.expiresAt());
        assertEquals(technician, second.user());
    }

    @Test
    void rotatedTokenIsAcceptedOnlyDuringGracePeriod() {
        RefreshTokenService.IssuedToken first = service.issue(technician);
        service.rotate(first.value());

        clock.advance(Duration.ofMinutes(2));
        assertNotNull(service.rotate(first.value()));

        clock.advance(Duration.ofMinutes(10));
        assertUnauthorized(() -> service.rotate(first.value()));
    }

    @Test
    void expiredRevokedOrInactiveTokensAreRejected() {
        RefreshTokenService.IssuedToken expiring = service.issue(technician);
        clock.advance(Duration.ofDays(15));
        assertUnauthorized(() -> service.rotate(expiring.value()));

        RefreshTokenService.IssuedToken revoked = service.issue(technician);
        service.revoke(revoked.value());
        assertUnauthorized(() -> service.rotate(revoked.value()));

        RefreshTokenService.IssuedToken inactive = service.issue(technician);
        technician.setActivo(false);
        assertUnauthorized(() -> service.rotate(inactive.value()));

        assertUnauthorized(() -> service.rotate("token-inventado"));
        assertUnauthorized(() -> service.rotate(null));
    }

    private static void assertUnauthorized(org.junit.jupiter.api.function.Executable executable) {
        ResponseStatusException error = assertThrows(ResponseStatusException.class, executable);
        assertEquals(HttpStatus.UNAUTHORIZED, error.getStatusCode());
    }

    private static final class MutableClock extends Clock {
        private Instant now;

        MutableClock(Instant now) {
            this.now = now;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
