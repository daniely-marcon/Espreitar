package com.example.espreitar.model.dto;

import com.example.espreitar.model.Perfil;
import com.example.espreitar.model.Provedor;

import java.time.LocalDateTime;

public record UsuarioAdminResumo(
        Long id,
        String nome,
        String email,
        Perfil perfil,
        Provedor provedor,
        String fotoPerfil,
        LocalDateTime dataCriacao,
        Long totalAvaliacoes
) {
}
