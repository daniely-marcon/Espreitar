package com.example.espreitar.model.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record AvaliacaoEntrada(
        @Min(0) @Max(5) Integer nota,
        @Size(max = 5000) String comentario,
        boolean spoiler
) {
}
