package com.agrosentinel.agrosentinel.services;

import java.time.Clock;
import java.time.Duration;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.agrosentinel.agrosentinel.models.ClientOperation;
import com.agrosentinel.agrosentinel.repositories.ClientOperationRepository;

/**
 * Idempotencia para la cola offline: la misma operación (misma clave y usuario)
 * se ejecuta una sola vez aunque el dispositivo la reenvíe por haber perdido la respuesta.
 */
@Service
public class ClientOperationService {
    /** Si una operación quedó "en proceso" más de este tiempo, se asume que el servidor se cayó. */
    static final Duration STALE_IN_PROGRESS = Duration.ofMinutes(5);
    static final Duration RETENTION = Duration.ofDays(30);

    public enum Outcome { STARTED, REPLAY, IN_PROGRESS, KEY_REUSED }

    public record Decision(Outcome outcome, ClientOperation operation) {
    }

    private final ClientOperationRepository repository;
    private final Clock clock;

    @Autowired
    public ClientOperationService(ClientOperationRepository repository) {
        this(repository, Clock.systemUTC());
    }

    ClientOperationService(ClientOperationRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    /** Registra el inicio de la operación o indica qué hacer si ya se había recibido. */
    public Decision begin(String userEmail, String key, String method, String path) {
        for (int attempt = 0; attempt < 2; attempt++) {
            Optional<ClientOperation> existing = repository.findByUserEmailAndIdempotencyKey(userEmail, key);
            if (existing.isPresent()) {
                Decision decision = evaluate(existing.get(), method, path);
                if (decision != null) return decision;
                continue;
            }
            ClientOperation operation = new ClientOperation();
            operation.setUserEmail(userEmail);
            operation.setIdempotencyKey(key);
            operation.setHttpMethod(method);
            operation.setRequestPath(truncate(path, 500));
            operation.setStatus(ClientOperation.IN_PROGRESS);
            operation.setCreatedAt(clock.instant());
            try {
                return new Decision(Outcome.STARTED, repository.saveAndFlush(operation));
            } catch (DataIntegrityViolationException raceWithSameKey) {
                // Otra petición con la misma clave llegó al mismo tiempo: se reevalúa.
            }
        }
        return new Decision(Outcome.IN_PROGRESS, null);
    }

    private Decision evaluate(ClientOperation operation, String method, String path) {
        boolean sameRequest = operation.getHttpMethod().equalsIgnoreCase(method)
                && operation.getRequestPath().equals(truncate(path, 500));
        if (!sameRequest) {
            return new Decision(Outcome.KEY_REUSED, operation);
        }
        if (ClientOperation.COMPLETED.equals(operation.getStatus())) {
            return new Decision(Outcome.REPLAY, operation);
        }
        if (operation.getCreatedAt().plus(STALE_IN_PROGRESS).isAfter(clock.instant())) {
            return new Decision(Outcome.IN_PROGRESS, operation);
        }
        repository.delete(operation);
        return null;
    }

    public void complete(ClientOperation operation, int status, String contentType, String body) {
        operation.setStatus(ClientOperation.COMPLETED);
        operation.setResponseStatus(status);
        operation.setResponseContentType(truncate(contentType, 150));
        operation.setResponseBody(body);
        operation.setCompletedAt(clock.instant());
        repository.save(operation);
        if (Math.floorMod(operation.getId() == null ? 0 : operation.getId(), 200L) == 0) {
            repository.deleteCreatedBefore(clock.instant().minus(RETENTION));
        }
    }

    /** La operación falló: se libera la clave para que pueda reintentarse. */
    public void abandon(ClientOperation operation) {
        repository.delete(operation);
    }

    private static String truncate(String value, int max) {
        if (value == null) return null;
        return value.length() <= max ? value : value.substring(0, max);
    }
}
