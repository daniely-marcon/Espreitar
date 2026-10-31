package com.example.espreitar.controller;

import com.example.espreitar.model.Midia;
import com.example.espreitar.model.StatusSolicitacao;
import com.example.espreitar.model.dto.SolicitacaoEntrada;
import com.example.espreitar.model.dto.SolicitacaoResposta;
import com.example.espreitar.service.SolicitacaoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/solicitacoes")
@RequiredArgsConstructor
public class SolicitacaoController {
    private final SolicitacaoService solicitacaoService;

    @PostMapping
    public SolicitacaoResposta criar(
            Authentication autenticacao, @Valid @RequestBody SolicitacaoEntrada solicitacao) {
        return solicitacaoService.criarSolicitacao(autenticacao.getName(), solicitacao);
    }

    @GetMapping("/fila")
    public List<SolicitacaoResposta> listarPorStatus(
            @RequestParam(defaultValue = "PENDENTE") StatusSolicitacao status) {
        return solicitacaoService.listarPorStatus(status);
    }

    @GetMapping("/minhas")
    public List<SolicitacaoResposta> listarMinhas(Authentication autenticacao) {
        return solicitacaoService.listarDoUsuario(autenticacao.getName());
    }

    @PostMapping("/{id}/aprovar")
    public Midia aprovar(@PathVariable Long id) {
        return solicitacaoService.aprovarSolicitacao(id);
    }

    @DeleteMapping("/{id}/rejeitar")
    public void rejeitar(@PathVariable Long id) {
        solicitacaoService.rejeitarSolicitacao(id);
    }
}
