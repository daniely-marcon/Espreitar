package com.example.espreitar.controller;

import com.example.espreitar.model.dto.DenunciaResposta;
import com.example.espreitar.service.DenunciaService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/denuncias")
@RequiredArgsConstructor
public class DenunciaController {
    private final DenunciaService denunciaService;

    @PostMapping
    public DenunciaResposta criar(
            Authentication autenticacao, @RequestParam Long avaliacaoId) {
        return denunciaService.criarDenuncia(autenticacao.getName(), avaliacaoId);
    }

    @GetMapping
    public List<DenunciaResposta> listar() {
        return denunciaService.listarTodas();
    }

    @DeleteMapping("/{id}")
    public void manter(@PathVariable Long id) {
        denunciaService.excluirDenuncia(id);
    }

    @DeleteMapping("/avaliacao/{avaliacaoId}")
    public void limparDaAvaliacao(@PathVariable Long avaliacaoId) {
        denunciaService.limparDenunciasDaAvaliacao(avaliacaoId);
    }
}
