package com.example.espreitar.controller;

import com.example.espreitar.model.Denuncia;
import com.example.espreitar.service.DenunciaService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/denuncias")
@RequiredArgsConstructor
public class DenunciaController {

    private final DenunciaService denunciaService;

    @PostMapping
    public Denuncia criar(
            @RequestParam Long usuarioId,
            @RequestParam Long avaliacaoId) {
        return denunciaService.criarDenuncia(usuarioId, avaliacaoId);
    }

    @GetMapping
    public List<Denuncia> listar() {
        return denunciaService.listarTodas();
    }

    @DeleteMapping("/{id}")
    public void excluir(@PathVariable Long id) {
        denunciaService.excluirDenuncia(id);
    }
}