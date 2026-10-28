package com.example.espreitar.controller;

import com.example.espreitar.model.Midia;
import com.example.espreitar.model.Solicitacao;
import com.example.espreitar.model.StatusSolicitacao;
import com.example.espreitar.service.SolicitacaoService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/solicitacoes")
@RequiredArgsConstructor
public class SolicitacaoController {

    private final SolicitacaoService solicitacaoService;

    @PostMapping
    public Solicitacao criar(@RequestParam Long usuarioId, @RequestBody Solicitacao solicitacao) {
        return solicitacaoService.criarSolicitacao(usuarioId, solicitacao);
    }

    @GetMapping("/fila")
    public List<Solicitacao> listarPorStatus(@RequestParam StatusSolicitacao status) {
        return solicitacaoService.listarPorStatus(status);
    }

    @GetMapping("/usuario/{usuarioId}")
    public List<Solicitacao> listarPorUsuario(@PathVariable Long usuarioId) {
        return solicitacaoService.listarPorUsuario(usuarioId);
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