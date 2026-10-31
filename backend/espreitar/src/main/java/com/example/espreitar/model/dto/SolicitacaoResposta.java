package com.example.espreitar.model.dto;

import com.example.espreitar.model.StatusSolicitacao;
import com.example.espreitar.model.TipoMidia;

import java.time.LocalDateTime;

public record SolicitacaoResposta(
        Long id,
        Long usuarioId,
        String nomeUsuario,
        String titulo,
        String idExterno,
        Integer ano,
        TipoMidia tipo,
        String sinopse,
        String cartaz,
        String trailerUrl,
        Double notaMediaExterna,
        Integer totalAvaliacoesExternas,
        StatusSolicitacao status,
        Integer nota,
        String comentario,
        boolean spoiler,
        LocalDateTime dataCriacao
) {
}
