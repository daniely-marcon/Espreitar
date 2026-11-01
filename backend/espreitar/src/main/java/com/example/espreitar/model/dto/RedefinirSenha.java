package com.example.espreitar.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RedefinirSenha(@NotBlank String token, @NotBlank @Size(max = 72) String senha) {
}
