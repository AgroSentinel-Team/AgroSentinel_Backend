package com.agrosentinel.agrosentinel.repositories;
import org.springframework.data.jpa.repository.JpaRepository;

import com.agrosentinel.agrosentinel.models.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    boolean existsByEmail(String email);
    Usuario findByEmail(String email);
}
