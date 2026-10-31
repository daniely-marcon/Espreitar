package com.example.espreitar.controller;

import com.example.espreitar.model.dto.AvaliacaoEntrada;
import com.example.espreitar.model.dto.AvaliacaoResposta;
import com.example.espreitar.service.AvaliacaoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/avaliacoes")
@RequiredArgsConstructor
public class AvaliacaoController {
    private final AvaliacaoService avaliacaoService;

    @PostMapping("/midia/{midiaId}")
    public AvaliacaoResposta cadastrar(
            Authentication autenticacao,
            @PathVariable Long midiaId,
            @Valid @RequestBody AvaliacaoEntrada dados) {
        return avaliacaoService.cadastrar(autenticacao.getName(), midiaId, dados);
    }

    @GetMapping("/midia/{midiaId}")
    public List<AvaliacaoResposta> listarPorMidia(
            @PathVariable Long midiaId,
            Authentication autenticacao,
            @RequestParam(required = false) Boolean spoiler) {
        return avaliacaoService.listarDaMidia(
                midiaId, autenticacao == null ? null : autenticacao.getName(), spoiler);
    }

    @GetMapping("/minhas")
    public List<AvaliacaoResposta> listarMinhas(Authentication autenticacao) {
        return avaliacaoService.listarDoUsuario(autenticacao.getName());
    }

    @PutMapping("/{id}")
    public AvaliacaoResposta atualizar(
            Authentication autenticacao, @PathVariable Long id,
            @Valid @RequestBody AvaliacaoEntrada dados) {
        return avaliacaoService.atualizar(autenticacao.getName(), isAdmin(autenticacao), id, dados);
    }

    @DeleteMapping("/{id}")
    public void excluir(Authentication autenticacao, @PathVariable Long id) {
        avaliacaoService.excluir(autenticacao.getName(), isAdmin(autenticacao), id);
    }

    private boolean isAdmin(Authentication autenticacao) {
        return autenticacao.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority).anyMatch("ROLE_ADMIN"::equals);
    }
}
