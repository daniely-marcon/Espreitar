package com.example.espreitar.model.dto;

import com.example.espreitar.model.Perfil;
import jakarta.validation.constraints.NotNull;

public record AtualizarPerfilAdmin(@NotNull Perfil perfil) {
}
