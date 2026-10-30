package com.example.espreitar.model.dto;

import com.example.espreitar.model.StatusSolicitacao;
import com.example.espreitar.model.TipoMidia;

import java.time.LocalDateTime;

public record SolicitacaoResposta(
        Long id,
        Long usuarioId,
        String nomeUsuario,
        String titulo,
        Integer ano,
        TipoMidia tipo,
        String sinopse,
        StatusSolicitacao status,
        Integer nota,
        String comentario,
        boolean spoiler,
        LocalDateTime dataCriacao
) {
}
