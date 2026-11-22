package com.agrosentinel.agrosentinel.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.agrosentinel.agrosentinel.models.ClientOperation;
import com.agrosentinel.agrosentinel.repositories.ClientOperationRepository;
import com.agrosentinel.agrosentinel.services.ClientOperationService;

import jakarta.servlet.FilterChain;

class IdempotencyFilterTest {
    private final Map<String, ClientOperation> stored = new HashMap<>();
    private final AtomicInteger executions = new AtomicInteger();
    private IdempotencyFilter filter;
    private int nextStatus;

    @BeforeEach
    void setUp() {
        ClientOperationRepository repository = mock(ClientOperationRepository.class);
        AtomicLong ids = new AtomicLong();
        when(repository.findByUserEmailAndIdempotencyKey(anyString(), anyString()))
                .thenAnswer(invocation -> Optional.ofNullable(
                        stored.get(invocation.getArgument(0) + "|" + invocation.getArgument(1))));
        when(repository.saveAndFlush(any(ClientOperation.class))).thenAnswer(invocation -> save(invocation.getArgument(0), ids));
        when(repository.save(any(ClientOperation.class))).thenAnswer(invocation -> save(invocation.getArgument(0), ids));
        doAnswer(invocation -> {
            ClientOperation operation = invocation.getArgument(0);
            stored.remove(operation.getUserEmail() + "|" + operation.getIdempotencyKey());
            return null;
        }).when(repository).delete(any(ClientOperation.class));

        filter = new IdempotencyFilter(new ClientOperationService(repository));
        nextStatus = 200;
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("Tecnico@Example.com", null, List.of()));
    }

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    private ClientOperation save(ClientOperation operation, AtomicLong ids) {
        if (operation.getId() == null) operation.setId(ids.incrementAndGet());
        stored.put(operation.getUserEmail() + "|" + operation.getIdempotencyKey(), operation);
        return operation;
    }

    private final FilterChain chain = (request, response) -> {
        executions.incrementAndGet();
        MockHttpServletResponse servletResponse = (MockHttpServletResponse) unwrap(response);
        jakarta.servlet.http.HttpServletResponse http = (jakarta.servlet.http.HttpServletResponse) response;
        http.setStatus(nextStatus);
        http.setContentType("application/json");
        http.getOutputStream().write("{\"id\":7,\"estado\":\"EN_CIERRE\"}".getBytes(StandardCharsets.UTF_8));
        assertTrue(servletResponse != null);
    };

    private static Object unwrap(Object response) {
        Object current = response;
        while (current instanceof jakarta.servlet.ServletResponseWrapper wrapper) {
            current = wrapper.getResponse();
        }
        return current;
    }

    private MockHttpServletResponse send(String key, String path) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("PUT", path);
        request.addHeader(IdempotencyFilter.HEADER, key);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, chain);
        return response;
    }

    @Test
    void aRepeatedOperationReturnsTheSavedResponseWithoutRunningAgain() throws Exception {
        MockHttpServletResponse first = send("op-12345678", "/api/failure-reports/7/intervention");
        MockHttpServletResponse second = send("op-12345678", "/api/failure-reports/7/intervention");

        assertEquals(1, executions.get());
        assertEquals(200, second.getStatus());
        assertEquals(first.getContentAsString(), second.getContentAsString());
        assertEquals("true", second.getHeader(IdempotencyFilter.REPLAYED_HEADER));
    }

    @Test
    void aFailedOperationReleasesTheKeySoItCanBeRetried() throws Exception {
        nextStatus = 409;
        assertEquals(409, send("op-retry-0001", "/api/failure-reports/7/intervention").getStatus());

        nextStatus = 200;
        assertEquals(200, send("op-retry-0001", "/api/failure-reports/7/intervention").getStatus());
        assertEquals(2, executions.get());
    }

    @Test
    void anOperationStillInProgressAsksTheClientToRetryLater() throws Exception {
        ClientOperation running = new ClientOperation();
        running.setUserEmail("tecnico@example.com");
        running.setIdempotencyKey("op-running-01");
        running.setHttpMethod("PUT");
        running.setRequestPath("/api/failure-reports/7/intervention");
        running.setStatus(ClientOperation.IN_PROGRESS);
        running.setCreatedAt(java.time.Instant.now());
        stored.put("tecnico@example.com|op-running-01", running);

        MockHttpServletResponse response = send("op-running-01", "/api/failure-reports/7/intervention");

        assertEquals(409, response.getStatus());
        assertTrue(response.getContentAsString().contains("OPERATION_IN_PROGRESS"));
        assertEquals(0, executions.get());
    }

    @Test
    void reusingAKeyForAnotherRequestIsRejected() throws Exception {
        send("op-reused-001", "/api/failure-reports/7/intervention");
        MockHttpServletResponse response = send("op-reused-001", "/api/failure-reports/8/intervention");

        assertEquals(422, response.getStatus());
        assertEquals(1, executions.get());
    }

    @Test
    void requestsWithoutKeyOrToAuthAreNotTracked() throws Exception {
        MockHttpServletRequest noKey = new MockHttpServletRequest("PUT", "/api/failure-reports/7/intervention");
        filter.doFilter(noKey, new MockHttpServletResponse(), chain);
        MockHttpServletRequest auth = new MockHttpServletRequest("POST", "/api/auth/login");
        auth.addHeader(IdempotencyFilter.HEADER, "op-auth-00001");
        filter.doFilter(auth, new MockHttpServletResponse(), chain);

        assertEquals(2, executions.get());
        assertTrue(stored.isEmpty());
    }
}
