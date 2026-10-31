package com.example.espreitar.model.dto;

import com.example.espreitar.model.TipoMidia;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SolicitacaoEntrada(
        @NotBlank @Size(max = 50) String idExterno,
        @NotNull TipoMidia tipo,
        @NotNull @Min(1) @Max(5) Integer nota,
        @Size(max = 5000) String comentario,
        boolean spoiler
) {
}
