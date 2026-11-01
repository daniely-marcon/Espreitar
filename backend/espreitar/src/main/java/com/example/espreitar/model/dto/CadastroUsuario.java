package com.example.espreitar.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CadastroUsuario(
        @NotBlank @Size(max = 100) String nome,
        @NotBlank @Email @Size(max = 150) String email,
        @NotBlank @Size(max = 72) String senha,
        @Size(max = 50000) String fotoPerfil
) {
}
