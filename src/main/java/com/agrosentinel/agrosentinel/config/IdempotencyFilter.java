package com.agrosentinel.agrosentinel.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import com.agrosentinel.agrosentinel.models.ClientOperation;
import com.agrosentinel.agrosentinel.services.ClientOperationService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Hace seguras las repeticiones de operaciones enviadas con la cabecera {@code Idempotency-Key}.
 *
 * <ul>
 *   <li>Primera vez: se ejecuta y, si sale bien, se guarda la respuesta.</li>
 *   <li>Repetida y terminada: se devuelve la respuesta guardada sin volver a ejecutar.</li>
 *   <li>Repetida mientras la primera sigue en proceso: 409 {@code OPERATION_IN_PROGRESS} (reintentar luego).</li>
 *   <li>Si la operación falla, la clave se libera para poder reintentarla.</li>
 * </ul>
 */
public class IdempotencyFilter extends OncePerRequestFilter {
    public static final String HEADER = "Idempotency-Key";
    public static final String REPLAYED_HEADER = "Idempotency-Replayed";
    private static final Set<String> MUTATING_METHODS = Set.of("POST", "PUT", "PATCH", "DELETE");
    private static final Pattern VALID_KEY = Pattern.compile("[A-Za-z0-9_-]{8,100}");

    private final ClientOperationService operations;

    public IdempotencyFilter(ClientOperationService operations) {
        this.operations = operations;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return request.getHeader(HEADER) == null
                || !MUTATING_METHODS.contains(request.getMethod())
                || path == null
                || !path.startsWith("/api/")
                || path.startsWith("/api/auth/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String key = request.getHeader(HEADER).trim();
        if (!VALID_KEY.matcher(key).matches()) {
            writeJson(response, HttpServletResponse.SC_BAD_REQUEST, "INVALID_IDEMPOTENCY_KEY",
                    "La clave de la operación no es válida.");
            return;
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            chain.doFilter(request, response);
            return;
        }

        ClientOperationService.Decision decision = operations.begin(
                authentication.getName().toLowerCase(), key, request.getMethod(), request.getRequestURI());
        switch (decision.outcome()) {
            case REPLAY -> replay(response, decision.operation());
            case IN_PROGRESS -> writeJson(response, HttpServletResponse.SC_CONFLICT, "OPERATION_IN_PROGRESS",
                    "La operación todavía se está procesando. Se reintentará en un momento.");
            case KEY_REUSED -> writeJson(response, 422, "IDEMPOTENCY_KEY_REUSED",
                    "Esta clave de operación ya se usó para otra solicitud.");
            case STARTED -> execute(request, response, chain, decision.operation());
        }
    }

    private void execute(HttpServletRequest request, HttpServletResponse response, FilterChain chain,
            ClientOperation operation) throws ServletException, IOException {
        ContentCachingResponseWrapper wrapper = new ContentCachingResponseWrapper(response);
        boolean recorded = false;
        try {
            chain.doFilter(request, wrapper);
            int status = wrapper.getStatus();
            if (status >= 200 && status < 300) {
                String body = new String(wrapper.getContentAsByteArray(), StandardCharsets.UTF_8);
                operations.complete(operation, status, wrapper.getContentType(), body);
                recorded = true;
            }
        } finally {
            if (!recorded) {
                try {
                    operations.abandon(operation);
                } catch (RuntimeException cleanupError) {
                    logger.warn("No se pudo liberar la clave de idempotencia " + operation.getIdempotencyKey(),
                            cleanupError);
                }
            }
            wrapper.copyBodyToResponse();
        }
    }

    private void replay(HttpServletResponse response, ClientOperation operation) throws IOException {
        response.setStatus(operation.getResponseStatus() == null ? 200 : operation.getResponseStatus());
        response.setHeader(REPLAYED_HEADER, "true");
        if (operation.getResponseContentType() != null) {
            response.setContentType(operation.getResponseContentType());
        }
        if (operation.getResponseBody() != null) {
            byte[] body = operation.getResponseBody().getBytes(StandardCharsets.UTF_8);
            response.setContentLength(body.length);
            response.getOutputStream().write(body);
        }
    }

    private void writeJson(HttpServletResponse response, int status, String code, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"code\":\"" + code + "\",\"message\":\"" + message + "\"}");
    }
}
