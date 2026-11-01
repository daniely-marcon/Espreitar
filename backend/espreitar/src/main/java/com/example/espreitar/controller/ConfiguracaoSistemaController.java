package com.example.espreitar.controller;

import com.example.espreitar.model.dto.LimiteDenuncias;
import com.example.espreitar.service.ConfiguracaoSistemaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/configuracoes/admin/denuncias")
@RequiredArgsConstructor
public class ConfiguracaoSistemaController {
    private final ConfiguracaoSistemaService configuracaoSistemaService;

    @GetMapping
    public LimiteDenuncias buscarLimiteDenuncias() {
        return configuracaoSistemaService.buscarLimiteDenuncias();
    }

    @PutMapping
    public LimiteDenuncias atualizarLimiteDenuncias(@Valid @RequestBody LimiteDenuncias dados) {
        return configuracaoSistemaService.atualizarLimiteDenuncias(dados);
    }
}
