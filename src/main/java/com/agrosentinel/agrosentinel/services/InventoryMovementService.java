package com.agrosentinel.agrosentinel.services;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
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
import com.agrosentinel.agrosentinel.models.Usuario;
import com.agrosentinel.agrosentinel.models.WorkGroup;
import com.agrosentinel.agrosentinel.repositories.InventoryItemRepository;
import com.agrosentinel.agrosentinel.repositories.InventoryMovementRepository;
import com.agrosentinel.agrosentinel.repositories.UsuarioRepository;
import com.agrosentinel.agrosentinel.repositories.WorkGroupRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InventoryMovementService {
    private final InventoryItemRepository itemRepository;
    private final InventoryMovementRepository movementRepository;
    private final WorkGroupRepository workGroupRepository;
    private final UsuarioRepository usuarioRepository;

    public List<InventoryItemResponse> findAllItems() {
        return itemRepository.findAll().stream().map(this::toItemResponse).toList();
    }

    public List<InventoryItemResponse> findItems(Long groupId, boolean central, Authentication authentication) {
        if (groupId == null && !central) {
            return findAllItems();
        }
        if (central) {
            return itemRepository.findAllByWorkGroupIsNullOrderByNameAsc().stream()
                    .map(this::toItemResponse).toList();
        }
        if (!canViewAnyZone(authentication)) {
            requireGroupAccess(groupId, authentication);
        } else {
            workGroupRepository.findByIdAndDeletedAtIsNull(groupId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Grupo o zona no encontrado"));
        }
        return itemRepository.findAllByWorkGroup_IdOrderByNameAsc(groupId).stream()
                .map(this::toItemResponse).toList();
    }

    @Transactional
    public InventoryMovementResponse registerInitialEntry(
            InventoryInitialEntryRequest request, String responsable, String rol) {
        return registerInitialEntry(request, responsable, rol, null);
    }

    @Transactional
    public InventoryMovementResponse registerInitialEntry(
            InventoryInitialEntryRequest request,
            String responsable,
            String rol,
            Authentication authentication) {
        InventoryItem item = new InventoryItem();
        item.setName(request.item().trim());
        item.setCategory(request.category().trim());
        item.setUnit(request.unit().trim());
        item.setMinimum(request.minimum());
        item.setStock(request.cantidad());
        if (request.groupId() != null) {
            item.setWorkGroup(requireGroupAccess(request.groupId(), authentication));
        }
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
            Long itemId, InventoryMovementRequest request, String responsable, String rol) {
        return registerMovement(itemId, request, responsable, rol, null);
    }

    @Transactional
    public InventoryMovementResponse registerMovement(
            Long itemId,
            InventoryMovementRequest request,
            String responsable,
            String rol,
            Authentication authentication) {
        InventoryItem item = itemRepository.findByIdForUpdate(itemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ítem de inventario no encontrado"));
        if (item.getWorkGroup() != null && authentication != null) {
            requireGroupAccess(item.getWorkGroup().getId(), authentication);
        }
        boolean productOutsideSelectedScope =
                (Boolean.TRUE.equals(request.central()) && item.getWorkGroup() != null)
                        || (request.groupId() != null && (item.getWorkGroup() == null
                                || !Objects.equals(item.getWorkGroup().getId(), request.groupId())));
        if (productOutsideSelectedScope) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "El producto no pertenece a la zona seleccionada");
        }
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

    public List<InventoryMovementResponse> findMovements(Long groupId, boolean central, Authentication authentication) {
        if (groupId == null && !central) {
            return findAllMovements();
        }
        List<InventoryMovement> movements;
        if (central) {
            movements = movementRepository.findAllForCentralHistory();
        } else if (canViewAnyZone(authentication)) {
            workGroupRepository.findByIdAndDeletedAtIsNull(groupId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Grupo o zona no encontrado"));
            movements = movementRepository.findAllForGroupHistory(groupId);
        } else {
            requireGroupAccess(groupId, authentication);
            movements = movementRepository.findAllForGroupHistory(groupId);
        }
        return movements.stream()
                .map(movement -> toResponse(movement, movement.getItem().getStock()))
                .toList();
    }

    private boolean canViewAnyZone(Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority -> authority.equals("ROLE_ADMIN") || authority.equals("ROLE_ADMINISTRADOR")
                        || authority.equals("ROLE_TECNICO") || authority.equals("ROLE_ALMACENISTA")
                        || authority.equals("ROLE_SUPERVISOR"));
    }

    private WorkGroup requireGroupAccess(Long groupId, Authentication authentication) {
        WorkGroup group = workGroupRepository.findByIdAndDeletedAtIsNull(groupId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Grupo o zona no encontrado"));
        if (authentication == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No autenticado");
        }
        boolean administrator = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority -> authority.equals("ROLE_ADMIN") || authority.equals("ROLE_ADMINISTRADOR"));
        if (administrator) {
            return group;
        }
        Usuario user = usuarioRepository.findByEmail(authentication.getName());
        if (user == null || group.getMembers().stream().noneMatch(member -> Objects.equals(member.getId(), user.getId()))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No perteneces al grupo de inventario seleccionado");
        }
        return group;
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
                stockActual,
                movement.getItem().getWorkGroup() == null ? null : movement.getItem().getWorkGroup().getId(),
                movement.getItem().getWorkGroup() == null ? null : movement.getItem().getWorkGroup().getName());
    }

    private InventoryItemResponse toItemResponse(InventoryItem item) {
        return new InventoryItemResponse(
                item.getId(),
                item.getName(),
                item.getCategory(),
                item.getStock(),
                item.getUnit(),
                item.getMinimum(),
                item.getWorkGroup() == null ? null : item.getWorkGroup().getId(),
                item.getWorkGroup() == null ? null : item.getWorkGroup().getName());
    }
}
