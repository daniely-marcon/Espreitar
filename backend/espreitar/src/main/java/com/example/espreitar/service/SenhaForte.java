package com.example.espreitar.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;

public final class SenhaForte {
    private SenhaForte() {
    }

    public static void validar(String senha) {
        if (senha == null
                || senha.length() < 8
                || senha.length() > 72
                || senha.getBytes(StandardCharsets.UTF_8).length > 72
                || !senha.matches("(?s).*[a-z].*")
                || !senha.matches("(?s).*[A-Z].*")
                || !senha.matches("(?s).*\\d.*")
                || !senha.matches("(?s).*[^A-Za-z0-9].*")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A senha deve ter ao menos 8 caracteres, incluindo maiúscula, minúscula, número e símbolo.");
        }
    }
}
