package com.example.espreitar.repository;

import com.example.espreitar.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmail(String email);

    Optional<Usuario> findByGoogleId(String googleId);

    boolean existsByEmail(String email);
}
