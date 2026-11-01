package com.example.espreitar.repository;

import com.example.espreitar.model.TokenRedefinicaoSenha;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TokenRedefinicaoSenhaRepository extends JpaRepository<TokenRedefinicaoSenha, Long> {
    Optional<TokenRedefinicaoSenha> findByTokenHash(String tokenHash);

    void deleteByUsuarioId(Long usuarioId);
}
