package com.example.espreitar.model.dto;

public record BuscaExterna(
        String idExterno,
        String titulo,
        String ano,
        String tipo,
        String cartaz,
        Double notaMedia,
        Integer totalAvaliacoes
) {
}
