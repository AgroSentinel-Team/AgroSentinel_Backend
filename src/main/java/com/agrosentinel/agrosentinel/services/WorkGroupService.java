package com.agrosentinel.agrosentinel.services;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.agrosentinel.agrosentinel.dtos.WorkGroupMemberResponse;
import com.agrosentinel.agrosentinel.dtos.WorkGroupRequest;
import com.agrosentinel.agrosentinel.dtos.WorkGroupResponse;
import com.agrosentinel.agrosentinel.models.Usuario;
import com.agrosentinel.agrosentinel.models.WorkGroup;
import com.agrosentinel.agrosentinel.models.FailureStatus;
import com.agrosentinel.agrosentinel.models.PreventiveMaintenanceStatus;
import com.agrosentinel.agrosentinel.repositories.FailureReportRepository;
import com.agrosentinel.agrosentinel.repositories.PreventiveMaintenanceRepository;
import com.agrosentinel.agrosentinel.repositories.UsuarioRepository;
import com.agrosentinel.agrosentinel.repositories.WorkGroupRepository;


@Service
@Transactional(readOnly = true)
public class WorkGroupService {
    private static final int TECHNICIAN_WORKLOAD_CAPACITY = 3;
    private static final Set<FailureStatus> ACTIVE_FAILURE_STATUSES = Set.of(
            FailureStatus.PENDIENTE,
            FailureStatus.ASIGNADA,
            FailureStatus.EN_EJECUCION,
            FailureStatus.EN_CIERRE,
            FailureStatus.EN_REVISION,
            FailureStatus.ESPERA_REPUESTO);
    private static final Set<String> GRANTABLE_ROLES = Set.of(
            "TECNICO", "SUPERVISOR", "ALMACENISTA", "OPERATIVO_CAMPO", "OPERARIO_CAMPO");

    private final WorkGroupRepository workGroupRepository;
    private final UsuarioRepository usuarioRepository;
    private final UserRoleService userRoleService;
    private final FailureReportRepository failureReportRepository;
    private final PreventiveMaintenanceRepository preventiveMaintenanceRepository;

    @Autowired
    public WorkGroupService(
            WorkGroupRepository workGroupRepository,
            UsuarioRepository usuarioRepository,
            UserRoleService userRoleService,
            FailureReportRepository failureReportRepository,
            PreventiveMaintenanceRepository preventiveMaintenanceRepository) {
        this.workGroupRepository = workGroupRepository;
        this.usuarioRepository = usuarioRepository;
        this.userRoleService = userRoleService;
        this.failureReportRepository = failureReportRepository;
        this.preventiveMaintenanceRepository = preventiveMaintenanceRepository;
    }

    public WorkGroupService(
            WorkGroupRepository workGroupRepository,
            UsuarioRepository usuarioRepository,
            UserRoleService userRoleService,
            FailureReportRepository failureReportRepository) {
        this(workGroupRepository, usuarioRepository, userRoleService, failureReportRepository, null);
    }

    public List<WorkGroupResponse> findAll() {
        return workGroupRepository.findAllByDeletedAtIsNullOrderByNameAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    public List<WorkGroupResponse> findForMember(String email) {
        Usuario member = requireActiveUserByEmail(email);
        return workGroupRepository.findAllByMembers_IdAndDeletedAtIsNullOrderByNameAsc(member.getId()).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public WorkGroupResponse create(WorkGroupRequest request) {
        validateName(request.name(), null);
        WorkGroup group = new WorkGroup();
        applyRequest(group, request);
        return toResponse(workGroupRepository.save(group));
    }

    @Transactional
    public WorkGroupResponse update(Long id, WorkGroupRequest request) {
        WorkGroup group = requireActiveGroup(id);
        validateName(request.name(), id);
        applyRequest(group, request);
        return toResponse(workGroupRepository.save(group));
    }

    @Transactional
    public WorkGroupResponse addMember(Long groupId, Long userId) {
        WorkGroup group = requireActiveGroup(groupId);
        group.getMembers().add(requireActiveUser(userId));
        return toResponse(workGroupRepository.save(group));
    }

    @Transactional
    public WorkGroupResponse removeMember(Long groupId, Long userId) {
        WorkGroup group = requireActiveGroup(groupId);
        if (group.getLeader() != null && group.getLeader().getId().equals(userId)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Asigna otro líder antes de retirar al líder actual del grupo");
        }
        group.getMembers().removeIf(member -> member.getId().equals(userId));
        return toResponse(workGroupRepository.save(group));
    }

    @Transactional
    public WorkGroupResponse updateRoles(Long groupId, Set<String> requestedRoles) {
        WorkGroup group = requireActiveGroup(groupId);
        group.setGrantedRoles(normalizeRoles(requestedRoles));
        return toResponse(workGroupRepository.save(group));
    }

    @Transactional
    public void softDelete(Long id) {
        WorkGroup group = requireActiveGroup(id);
        group.setDeletedAt(LocalDateTime.now());
        workGroupRepository.save(group);
    }

    private void applyRequest(WorkGroup group, WorkGroupRequest request) {
        Usuario leader = requireActiveUser(request.leaderId());
        Set<Usuario> members = new HashSet<>();
        for (Long memberId : request.memberIds()) {
            members.add(requireActiveUser(memberId));
        }
        members.add(leader);

        group.setName(request.name().trim());
        group.setDescription(request.description() == null ? null : request.description().trim());
        group.setLeader(leader);
        group.setMembers(members);
        group.setGrantedRoles(normalizeRoles(request.grantedRoles()));
    }

    private Set<String> normalizeRoles(Set<String> requestedRoles) {
        Set<String> roles = new HashSet<>();
        for (String requestedRole : requestedRoles) {
            String role = requestedRole.trim().toUpperCase(Locale.ROOT).replaceFirst("^ROLE_", "");
            if (!GRANTABLE_ROLES.contains(role)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "El rol '" + requestedRole + "' no se puede asignar a un grupo");
            }
            roles.add(role);
        }
        return roles;
    }

    private void validateName(String requestedName, Long groupId) {
        workGroupRepository.findByNameIgnoreCaseAndDeletedAtIsNull(requestedName.trim())
                .filter(existing -> !existing.getId().equals(groupId))
                .ifPresent(existing -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un grupo activo con ese nombre");
                });
    }

    private Usuario requireActiveUser(Long id) {
        Usuario user = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        if (Boolean.FALSE.equals(user.getActivo())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se puede agregar un usuario inactivo");
        }
        return user;
    }

    private Usuario requireActiveUserByEmail(String email) {
        Usuario user = usuarioRepository.findByEmail(email);
        if (user == null || Boolean.FALSE.equals(user.getActivo())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario autenticado no disponible");
        }
        return user;
    }

    private WorkGroup requireActiveGroup(Long id) {
        return workGroupRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Grupo no encontrado"));
    }

    private WorkGroupResponse toResponse(WorkGroup group) {
        List<WorkGroupMemberResponse> members = group.getMembers().stream()
                .sorted(Comparator.comparing(Usuario::getNombre, String.CASE_INSENSITIVE_ORDER))
                .map(member -> toMemberResponse(group, member))
                .toList();
        return new WorkGroupResponse(
                group.getId(),
                group.getName(),
                group.getDescription(),
                group.getLeader() == null ? null : group.getLeader().getId(),
                group.getLeader() == null ? null : group.getLeader().getNombre(),
                members,
                Set.copyOf(group.getGrantedRoles()),
                group.getDeletedAt() == null);
    }

    private WorkGroupMemberResponse toMemberResponse(WorkGroup group, Usuario member) {
        Set<String> roles = userRoleService.getEffectiveRoles(member);
        boolean technician = roles.stream()
                .map(role -> role.toUpperCase(Locale.ROOT).replaceFirst("^ROLE_", ""))
                .anyMatch("TECNICO"::equals);
        long activeWorkload = technician
                ? failureReportRepository.countByAssignedTechnicianIdAndEstadoIn(
                        member.getId(), ACTIVE_FAILURE_STATUSES)
                : 0;
        if (technician && preventiveMaintenanceRepository != null) {
            activeWorkload += preventiveMaintenanceRepository.countByTechnician_IdAndStatusIn(
                    member.getId(), Set.of(
                            PreventiveMaintenanceStatus.SCHEDULED,
                            PreventiveMaintenanceStatus.PARTIALLY_COMPLETED));
        }
        return new WorkGroupMemberResponse(
                member.getId(),
                member.getNombre(),
                member.getEmail(),
                member.getRol(),
                roles,
                group.getLeader() != null && group.getLeader().getId().equals(member.getId()),
                activeWorkload,
                TECHNICIAN_WORKLOAD_CAPACITY,
                technician && !Boolean.FALSE.equals(member.getActivo())
                        && activeWorkload < TECHNICIAN_WORKLOAD_CAPACITY,
                !Boolean.FALSE.equals(member.getActivo()));
    }

}
