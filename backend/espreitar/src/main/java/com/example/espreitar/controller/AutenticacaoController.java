package com.example.espreitar.controller;

import com.example.espreitar.model.dto.AuthResposta;
import com.example.espreitar.model.dto.CadastroUsuario;
import com.example.espreitar.model.dto.LoginUsuario;
import com.example.espreitar.model.dto.TokenGoogle;
import com.example.espreitar.model.dto.RedefinirSenha;
import com.example.espreitar.model.dto.SolicitarRedefinicaoSenha;
import com.example.espreitar.service.AutenticacaoAplicacaoService;
import com.example.espreitar.service.RecuperacaoSenhaService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Value;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AutenticacaoController {
    private final AutenticacaoAplicacaoService autenticacao;
    private final RecuperacaoSenhaService recuperacaoSenhaService;
    private final String googleClientId;

    public AutenticacaoController(
            AutenticacaoAplicacaoService autenticacao,
            RecuperacaoSenhaService recuperacaoSenhaService,
            @Value("${google.client-id:}") String googleClientId) {
        this.autenticacao = autenticacao;
        this.recuperacaoSenhaService = recuperacaoSenhaService;
        this.googleClientId = googleClientId;
    }

    @org.springframework.web.bind.annotation.GetMapping("/config")
    public Map<String, String> configuracaoPublica() {
        return Map.of("googleClientId", googleClientId);
    }

    @PostMapping("/login")
    public AuthResposta login(@Valid @RequestBody LoginUsuario dados) {
        return autenticacao.login(dados.email(), dados.senha());
    }

    @PostMapping("/google")
    public AuthResposta google(@Valid @RequestBody TokenGoogle dados) {
        return autenticacao.autenticarGoogle(dados.credential());
    }

    @PostMapping("/registro")
    public AuthResposta registrar(@Valid @RequestBody CadastroUsuario dados) {
        return autenticacao.cadastrar(dados);
    }

    @PostMapping("/esqueci-senha")
    public Map<String, String> solicitarRedefinicao(@Valid @RequestBody SolicitarRedefinicaoSenha dados) {
        recuperacaoSenhaService.solicitar(dados);
        return Map.of("message", "Se houver uma conta com esse e-mail, enviaremos um link de redefinição.");
    }

    @PostMapping("/redefinir-senha")
    public Map<String, String> redefinirSenha(@Valid @RequestBody RedefinirSenha dados) {
        recuperacaoSenhaService.redefinir(dados);
        return Map.of("message", "Senha redefinida. Você já pode entrar com a nova senha.");
    }
}
