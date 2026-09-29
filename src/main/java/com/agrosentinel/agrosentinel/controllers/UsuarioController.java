package com.agrosentinel.agrosentinel.controllers;

import java.util.List;
import java.util.Set;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.agrosentinel.agrosentinel.models.Usuario;
import com.agrosentinel.agrosentinel.repositories.UsuarioRepository;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private static final Set<String> ROLES_VALIDOS = Set.of(
            "TECNICO", "SUPERVISOR", "ALMACENISTA", "OPERATIVO_CAMPO", "ADMINISTRADOR");

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioController(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping
    public List<UsuarioResponse> listarUsuarios() {
        return usuarioRepository.findAll().stream().map(this::toResponse).toList();
    }

    @PostMapping
    public ResponseEntity<UsuarioResponse> crearUsuario(@Valid @RequestBody CrearUsuarioRequest request) {
        validarRol(request.rol());
        if (usuarioRepository.existsByEmail(request.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El correo ya está registrado.");
        }

        Usuario usuario = new Usuario();
        usuario.setNombre(request.nombre().trim());
        usuario.setEmail(request.email().trim());
        usuario.setPassword(passwordEncoder.encode(request.password()));
        usuario.setRol(request.rol());
        usuario.setActivo(true);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(usuarioRepository.save(usuario)));
    }

    @PutMapping("/{id}")
    public UsuarioResponse actualizarUsuario(@PathVariable Long id, @Valid @RequestBody ActualizarUsuarioRequest request) {
        validarRol(request.rol());
        Usuario usuario = buscarUsuario(id);
        Usuario usuarioConCorreo = usuarioRepository.findByEmail(request.email().trim());
        if (usuarioConCorreo != null && !usuarioConCorreo.getId().equals(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El correo ya está registrado.");
        }

        usuario.setNombre(request.nombre().trim());
        usuario.setEmail(request.email().trim());
        usuario.setRol(request.rol());
        return toResponse(usuarioRepository.save(usuario));
    }

    @PatchMapping("/{id}/rol")
    public UsuarioResponse cambiarRol(@PathVariable Long id, @Valid @RequestBody RolRequest request) {
        validarRol(request.rol());
        Usuario usuario = buscarUsuario(id);
        usuario.setRol(request.rol());
        return toResponse(usuarioRepository.save(usuario));
    }

    @PatchMapping("/{id}/estado")
    public UsuarioResponse cambiarEstado(@PathVariable Long id, @Valid @RequestBody EstadoRequest request) {
        Usuario usuario = buscarUsuario(id);
        usuario.setActivo(request.activo());
        return toResponse(usuarioRepository.save(usuario));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarUsuario(@PathVariable Long id) {
        usuarioRepository.delete(buscarUsuario(id));
        return ResponseEntity.noContent().build();
    }

    private Usuario buscarUsuario(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado."));
    }

    private void validarRol(String rol) {
        if (!ROLES_VALIDOS.contains(rol)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El rol indicado no es válido.");
        }
    }

    private UsuarioResponse toResponse(Usuario usuario) {
        return new UsuarioResponse(
                usuario.getId(),
                usuario.getNombre(),
                usuario.getEmail(),
                usuario.getRol(),
                usuario.getActivo() == null || usuario.getActivo(),
                usuario.getUltimoAcceso());
    }

    public record CrearUsuarioRequest(
            @NotBlank String nombre,
            @NotBlank @Email String email,
            @NotBlank String password,
            @NotBlank String rol) {}

    public record ActualizarUsuarioRequest(
            @NotBlank String nombre,
            @NotBlank @Email String email,
            @NotBlank String rol) {}

    public record RolRequest(@NotBlank String rol) {}

    public record EstadoRequest(boolean activo) {}

    public record UsuarioResponse(
            Long id,
            String nombre,
            String email,
            String rol,
            boolean activo,
            java.time.LocalDateTime ultimoAcceso) {}
}