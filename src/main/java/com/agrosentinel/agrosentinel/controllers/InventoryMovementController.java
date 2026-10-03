package com.agrosentinel.agrosentinel.controllers;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.agrosentinel.agrosentinel.dtos.InventoryMovementRequest;
import com.agrosentinel.agrosentinel.dtos.InventoryMovementResponse;
import com.agrosentinel.agrosentinel.dtos.InventoryInitialEntryRequest;
import com.agrosentinel.agrosentinel.dtos.InventoryItemResponse;
import com.agrosentinel.agrosentinel.services.InventoryMovementService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/inventory")
@CrossOrigin(origins = "http://localhost:5173")
@RequiredArgsConstructor
public class InventoryMovementController {
    private final InventoryMovementService movementService;

    @GetMapping("/items")
    public List<InventoryItemResponse> findAllItems() {
        return movementService.findAllItems();
    }

    @PostMapping("/entries/new-item")
    @ResponseStatus(HttpStatus.CREATED)
    public InventoryMovementResponse registerInitialEntry(
            @Valid @RequestBody InventoryInitialEntryRequest request,
            Authentication authentication) {
        String role = getInventoryRole(authentication);
        return movementService.registerInitialEntry(request, authentication.getName(), role);
    }

    @PostMapping("/{id}/movements")
    @ResponseStatus(HttpStatus.CREATED)
    public InventoryMovementResponse registerMovement(
            @PathVariable Long id,
            @Valid @RequestBody InventoryMovementRequest request,
            Authentication authentication) {
        String role = getInventoryRole(authentication);

        return movementService.registerMovement(
                id, request, authentication.getName(), role);
    }

    private String getInventoryRole(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .filter(authority -> authority.equals("ROLE_ADMINISTRADOR")
                        || authority.equals("ROLE_ADMIN")
                        || authority.equals("ROLE_ALMACENISTA"))
                .findFirst()
                .orElseThrow(() -> new org.springframework.security.access.AccessDeniedException(
                        "Rol no autorizado para registrar movimientos"));
    }

    @GetMapping("/movements")
    public List<InventoryMovementResponse> findAllMovements() {
        return movementService.findAllMovements();
    }
}
