package com.example.espreitar.controller;

import com.example.espreitar.model.Avaliacao;
import com.example.espreitar.service.AvaliacaoService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/avaliacoes")
@RequiredArgsConstructor
public class AvaliacaoController {

    private final AvaliacaoService avaliacaoService;

    @PostMapping
    public Avaliacao cadastrar(
            @RequestParam Long usuarioId,
            @RequestParam Long midiaId,
            @RequestBody Avaliacao avaliacao) {
        return avaliacaoService.cadastrarAvaliacao(usuarioId, midiaId, avaliacao);
    }

    @GetMapping("/midia/{midiaId}")
    public List<Avaliacao> listarPorMidia(
            @PathVariable Long midiaId,
            @RequestParam(required = false) Long usuarioIdLogado,
            @RequestParam(required = false) Boolean spoiler) {
        return avaliacaoService.listarAvaliacoesDaMidia(midiaId, usuarioIdLogado, spoiler);
    }

    @PutMapping("/{id}")
    public Avaliacao atualizar(@PathVariable Long id, @RequestBody Avaliacao avaliacao) {
        return avaliacaoService.atualizarAvaliacao(id, avaliacao);
    }

    @DeleteMapping("/{id}")
    public void excluir(@PathVariable Long id) {
        avaliacaoService.excluirAvaliacao(id);
    }
}