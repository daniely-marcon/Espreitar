package com.example.espreitar.model.dto;

import com.example.espreitar.model.Perfil;

public record UsuarioResumo(Long id, String nome, String email, Perfil perfil, String fotoPerfil) {
}
