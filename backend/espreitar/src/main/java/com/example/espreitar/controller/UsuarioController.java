package com.example.espreitar.controller;

import com.example.espreitar.model.dto.AtualizarPerfil;
import com.example.espreitar.model.dto.AuthResposta;
import com.example.espreitar.model.dto.UsuarioResumo;
import com.example.espreitar.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/usuarios")
public class UsuarioController {
    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping("/me")
    public UsuarioResumo meuPerfil(Authentication autenticacao) {
        return usuarioService.buscarPerfil(autenticacao.getName());
    }

    @PutMapping("/me")
    public AuthResposta atualizarPerfil(
            Authentication autenticacao, @Valid @RequestBody AtualizarPerfil dados) {
        return usuarioService.atualizarPerfil(autenticacao.getName(), dados);
    }

    @GetMapping("/me/notificacoes")
    public List<String> notificacoes(Authentication autenticacao) {
        return usuarioService.listarNotificacoes(autenticacao.getName());
    }
}
