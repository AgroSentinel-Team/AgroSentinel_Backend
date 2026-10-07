package com.agrosentinel.agrosentinel.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.server.ResponseStatusException;

import com.agrosentinel.agrosentinel.dtos.InventoryMovementRequest;
import com.agrosentinel.agrosentinel.dtos.InventoryMovementResponse;
import com.agrosentinel.agrosentinel.dtos.InventoryInitialEntryRequest;
import com.agrosentinel.agrosentinel.models.InventoryItem;
import com.agrosentinel.agrosentinel.models.InventoryMovement;
import com.agrosentinel.agrosentinel.models.InventoryMovementType;
import com.agrosentinel.agrosentinel.models.WorkGroup;
import com.agrosentinel.agrosentinel.repositories.InventoryItemRepository;
import com.agrosentinel.agrosentinel.repositories.InventoryMovementRepository;
import com.agrosentinel.agrosentinel.repositories.UsuarioRepository;
import com.agrosentinel.agrosentinel.repositories.WorkGroupRepository;

import static org.mockito.Mockito.mock;

class InventoryMovementServiceTest {
    private InventoryItemRepository itemRepository;
    private InventoryMovementRepository movementRepository;
    private WorkGroupRepository workGroupRepository;
    private InventoryMovementService service;
    private InventoryItem item;

    @BeforeEach
    void setUp() {
        itemRepository = mock(InventoryItemRepository.class);
        movementRepository = mock(InventoryMovementRepository.class);
        workGroupRepository = mock(WorkGroupRepository.class);
        service = new InventoryMovementService(
                itemRepository, movementRepository, workGroupRepository, mock(UsuarioRepository.class));
        item = new InventoryItem();
        item.setId(7L);
        item.setName("Filtro de aceite");
        item.setStock(new BigDecimal("3.000"));
        when(itemRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(item));
        when(movementRepository.save(any(InventoryMovement.class))).thenAnswer(invocation -> {
            InventoryMovement movement = invocation.getArgument(0);
            movement.setId(12L);
            movement.setFecha(Instant.parse("2026-10-03T17:00:00Z"));
            return movement;
        });
    }

    @Test
    void registerEntryIncreasesStockAndStoresMovement() {
        InventoryMovementResponse response = service.registerMovement(
                7L,
                new InventoryMovementRequest(InventoryMovementType.ENTRADA, new BigDecimal("2.000"), "Recepción"),
                "admin@agrosentinel.test",
                "ROLE_ADMINISTRADOR");

        assertEquals(new BigDecimal("5.000"), item.getStock());
        assertEquals(new BigDecimal("5.000"), response.stockActual());
        assertEquals("administrador", response.rol());
        verify(itemRepository).save(item);
        verify(movementRepository).save(any(InventoryMovement.class));
    }

    @Test
    void registerExitRejectsQuantityAboveAvailableStock() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.registerMovement(
                        7L,
                        new InventoryMovementRequest(InventoryMovementType.SALIDA, new BigDecimal("3.001"), "Uso"),
                        "warehouse@agrosentinel.test",
                        "ROLE_ALMACENISTA"));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals(new BigDecimal("3.000"), item.getStock());
        verify(itemRepository, never()).save(item);
        verify(movementRepository, never()).save(any(InventoryMovement.class));
    }

    @Test
    void registerExitAllowsExactAvailableQuantity() {
        InventoryMovementResponse response = service.registerMovement(
                7L,
                new InventoryMovementRequest(InventoryMovementType.SALIDA, new BigDecimal("3.000"), "Uso"),
                "warehouse@agrosentinel.test",
                "ROLE_ALMACENISTA");

        assertEquals(new BigDecimal("0.000"), item.getStock());
        assertEquals(new BigDecimal("0.000"), response.stockActual());
        verify(movementRepository).save(any(InventoryMovement.class));
    }

    @Test
    void registerMovementRejectsItemOutsideSelectedGroup() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.registerMovement(
                        7L,
                        new InventoryMovementRequest(
                                InventoryMovementType.ENTRADA, new BigDecimal("1.000"), "Reposición", 9L, false),
                        "warehouse@agrosentinel.test",
                        "ROLE_ALMACENISTA"));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(itemRepository, never()).save(item);
        verify(movementRepository, never()).save(any(InventoryMovement.class));
    }

    @Test
    void createNewItemAndInitialEntryTogether() {
        when(itemRepository.save(any(InventoryItem.class))).thenAnswer(invocation -> {
            InventoryItem createdItem = invocation.getArgument(0);
            createdItem.setId(21L);
            return createdItem;
        });

        InventoryMovementResponse response = service.registerInitialEntry(
                new InventoryInitialEntryRequest(
                        "Semillas", "Insumos", "kg",
                        new BigDecimal("5.000"), new BigDecimal("20.000"), "Compra inicial", null),
                "warehouse@agrosentinel.test",
                "ROLE_ALMACENISTA");

        assertEquals(21L, response.itemId());
        assertEquals(InventoryMovementType.ENTRADA, response.tipo());
        assertEquals(new BigDecimal("20.000"), response.stockActual());
        verify(itemRepository).save(any(InventoryItem.class));
        verify(movementRepository).save(any(InventoryMovement.class));
    }

    @Test
    void createNewItemAndInitialEntryInSelectedGroup() {
        WorkGroup group = new WorkGroup();
        group.setId(9L);
        group.setName("Zona Norte");
        when(workGroupRepository.findByIdAndDeletedAtIsNull(9L)).thenReturn(Optional.of(group));
        when(itemRepository.save(any(InventoryItem.class))).thenAnswer(invocation -> {
            InventoryItem createdItem = invocation.getArgument(0);
            createdItem.setId(22L);
            return createdItem;
        });
        var authentication = new UsernamePasswordAuthenticationToken(
                "admin@agrosentinel.test", "not-used", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

        InventoryMovementResponse response = service.registerInitialEntry(
                new InventoryInitialEntryRequest(
                        "Filtro de aire", "Filtros", "und",
                        new BigDecimal("2.000"), new BigDecimal("8.000"), "Compra", 9L),
                "admin@agrosentinel.test",
                "ROLE_ADMIN",
                authentication);

        assertEquals(9L, response.groupId());
        verify(itemRepository).save(org.mockito.ArgumentMatchers.argThat(
                savedItem -> savedItem.getWorkGroup() != null
                        && savedItem.getWorkGroup().getId().equals(9L)));
    }
}
