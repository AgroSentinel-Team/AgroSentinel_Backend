package com.agrosentinel.agrosentinel.services;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.agrosentinel.agrosentinel.dtos.InventoryMovementRequest;
import com.agrosentinel.agrosentinel.dtos.InventoryMovementResponse;
import com.agrosentinel.agrosentinel.dtos.InventoryInitialEntryRequest;
import com.agrosentinel.agrosentinel.dtos.InventoryItemResponse;
import com.agrosentinel.agrosentinel.models.InventoryItem;
import com.agrosentinel.agrosentinel.models.InventoryMovement;
import com.agrosentinel.agrosentinel.models.InventoryMovementType;
import com.agrosentinel.agrosentinel.repositories.InventoryItemRepository;
import com.agrosentinel.agrosentinel.repositories.InventoryMovementRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InventoryMovementService {
    private final InventoryItemRepository itemRepository;
    private final InventoryMovementRepository movementRepository;

    public List<InventoryItemResponse> findAllItems() {
        return itemRepository.findAll().stream().map(this::toItemResponse).toList();
    }

    @Transactional
    public InventoryMovementResponse registerInitialEntry(
            InventoryInitialEntryRequest request,
            String responsable,
            String rol) {
        InventoryItem item = new InventoryItem();
        item.setName(request.item().trim());
        item.setCategory(request.category().trim());
        item.setUnit(request.unit().trim());
        item.setMinimum(request.minimum());
        item.setStock(request.cantidad());
        item = itemRepository.save(item);

        InventoryMovement movement = new InventoryMovement();
        movement.setItem(item);
        movement.setTipo(InventoryMovementType.ENTRADA);
        movement.setCantidad(request.cantidad());
        movement.setResponsable(responsable);
        movement.setRol(normalizeRole(rol));
        movement.setMotivo(request.motivo());
        movement = movementRepository.save(movement);

        return toResponse(movement, item.getStock());
    }

    @Transactional
    public InventoryMovementResponse registerMovement(
            Long itemId,
            InventoryMovementRequest request,
            String responsable,
            String rol) {
        InventoryItem item = itemRepository.findByIdForUpdate(itemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ítem de inventario no encontrado"));
        BigDecimal stock = item.getStock() == null ? BigDecimal.ZERO : item.getStock();

        if (request.tipo() == InventoryMovementType.SALIDA
                && request.cantidad().compareTo(stock) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La salida supera el stock disponible");
        }

        BigDecimal newStock = request.tipo() == InventoryMovementType.ENTRADA
                ? stock.add(request.cantidad())
                : stock.subtract(request.cantidad());
        item.setStock(newStock);
        itemRepository.save(item);

        InventoryMovement movement = new InventoryMovement();
        movement.setItem(item);
        movement.setTipo(request.tipo());
        movement.setCantidad(request.cantidad());
        movement.setResponsable(responsable);
        movement.setRol(normalizeRole(rol));
        movement.setMotivo(request.motivo());
        movement = movementRepository.save(movement);

        return toResponse(movement, newStock);
    }

    public List<InventoryMovementResponse> findAllMovements() {
        return movementRepository.findAllForHistory().stream()
                .map(movement -> toResponse(movement, movement.getItem().getStock()))
                .toList();
    }

    private String normalizeRole(String role) {
        String normalized = role.startsWith("ROLE_") ? role.substring(5) : role;
        return normalized.toLowerCase(Locale.ROOT);
    }

    private InventoryMovementResponse toResponse(InventoryMovement movement, BigDecimal stockActual) {
        return new InventoryMovementResponse(
                movement.getId(),
                movement.getItem().getId(),
                movement.getItem().getName(),
                movement.getItem().getUnit(),
                movement.getTipo(),
                movement.getCantidad(),
                movement.getResponsable(),
                movement.getRol(),
                movement.getMotivo(),
                movement.getFecha(),
                stockActual);
    }

    private InventoryItemResponse toItemResponse(InventoryItem item) {
        return new InventoryItemResponse(
                item.getId(),
                item.getName(),
                item.getCategory(),
                item.getStock(),
                item.getUnit(),
                item.getMinimum());
    }
}
