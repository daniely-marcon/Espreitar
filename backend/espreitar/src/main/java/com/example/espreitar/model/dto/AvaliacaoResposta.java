package com.example.espreitar.model.dto;

import java.time.LocalDateTime;

public record AvaliacaoResposta(
        Long id,
        Long usuarioId,
        String nomeUsuario,
        String fotoPerfil,
        Long midiaId,
        String tituloMidia,
        String cartaz,
        Integer nota,
        String comentario,
        boolean spoiler,
        LocalDateTime dataCriacao
) {
}
