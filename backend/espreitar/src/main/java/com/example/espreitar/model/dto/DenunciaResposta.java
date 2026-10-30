package com.example.espreitar.model.dto;

import java.time.LocalDateTime;

public record DenunciaResposta(
        Long id,
        Long avaliacaoId,
        String comentario,
        String autor,
        String tituloMidia,
        String denunciante,
        LocalDateTime dataCriacao
) {
}
