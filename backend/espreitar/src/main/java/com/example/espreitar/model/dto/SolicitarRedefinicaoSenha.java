package com.example.espreitar.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record SolicitarRedefinicaoSenha(@NotBlank @Email String email) {
}
