package com.example.espreitar.controller;

import com.example.espreitar.model.dto.AtualizarPerfil;
import com.example.espreitar.model.dto.AuthResposta;
import com.example.espreitar.model.dto.UsuarioResumo;
import com.example.espreitar.model.dto.UsuarioAdminResumo;
import com.example.espreitar.model.dto.AtualizarPerfilAdmin;
import com.example.espreitar.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

    @GetMapping("/admin")
    public List<UsuarioAdminResumo> listarParaAdmin() {
        return usuarioService.listarUsuariosParaAdmin();
    }

    @PutMapping("/admin/{id}/perfil")
    public void atualizarPerfilAdmin(
            Authentication autenticacao, @PathVariable Long id, @Valid @RequestBody AtualizarPerfilAdmin dados) {
        usuarioService.atualizarPerfilAdmin(autenticacao.getName(), id, dados);
    }

    @DeleteMapping("/admin/{id}")
    public void excluirUsuarioAdmin(Authentication autenticacao, @PathVariable Long id) {
        usuarioService.excluirUsuarioAdmin(autenticacao.getName(), id);
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

    @DeleteMapping("/me/notificacoes")
    public void limparNotificacoes(Authentication autenticacao) {
        usuarioService.limparNotificacoes(autenticacao.getName());
    }
}
