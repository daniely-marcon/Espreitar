package com.example.espreitar.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginUsuario(@NotBlank @Email String email, @NotBlank String senha) {
}
