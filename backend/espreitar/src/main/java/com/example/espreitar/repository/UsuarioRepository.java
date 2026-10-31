package com.example.espreitar.repository;

import com.example.espreitar.model.Usuario;
import com.example.espreitar.model.Perfil;
import com.example.espreitar.model.dto.UsuarioAdminResumo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmail(String email);

    Optional<Usuario> findByGoogleId(String googleId);

    List<Usuario> findByPerfil(Perfil perfil);

    boolean existsByEmail(String email);

    long countByPerfil(Perfil perfil);

    @Query("""
            SELECT new com.example.espreitar.model.dto.UsuarioAdminResumo(
                u.id, u.nome, u.email, u.perfil, u.provedor, u.fotoPerfil, u.dataCriacao,
                (SELECT COUNT(a.id) FROM Avaliacao a WHERE a.usuario = u))
            FROM Usuario u
            ORDER BY u.dataCriacao DESC
            """)
    List<UsuarioAdminResumo> listarResumoAdmin();
}
