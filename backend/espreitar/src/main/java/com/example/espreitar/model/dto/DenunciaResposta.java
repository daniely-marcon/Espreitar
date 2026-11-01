package com.example.espreitar.model.dto;

import com.example.espreitar.model.MotivoDenuncia;

import java.time.LocalDateTime;

public record DenunciaResposta(
        Long id,
        Long avaliacaoId,
        String comentario,
        String autor,
        String tituloMidia,
        String denunciante,
        MotivoDenuncia motivo,
        LocalDateTime dataCriacao,
        long totalDenuncias
) {
}
